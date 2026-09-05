package app.bottlenote.notification.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationMarkReadResult;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import app.bottlenote.user.event.payload.FollowActivityEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] ReviewReplyNotificationListener")
class ReviewReplyNotificationListenerTest {

  private static final Long REVIEW_ID = 10L;
  private static final Long REVIEW_AUTHOR_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long REPLY_ID = 100L;

  private RecordingNotificationService notificationService;
  private ReviewReplyNotificationListener listener;

  @BeforeEach
  void setUp() {
    notificationService = new RecordingNotificationService();
    listener = new ReviewReplyNotificationListener(notificationService);
  }

  @Nested
  @DisplayName("리뷰 댓글 알림을 생성할 때")
  class HandleReviewReplyNotification {

    @Test
    @DisplayName("다른 사용자가 댓글 달면 리뷰 작성자에게 알림을 보낸다")
    void handleReviewReplyNotification_whenOtherUserReplies_sendsNotificationToAuthor() {
      // given
      String content = "좋은 리뷰네요!";
      ReviewReplyActivityEvent event =
          new ReviewReplyActivityEvent(
              REVIEW_ID, 20L, REVIEW_AUTHOR_ID, OTHER_USER_ID, REPLY_ID, null, content);

      // when
      listener.handleReviewReplyNotification(event);

      // then
      assertThat(notificationService.messages).hasSize(1);
      NotificationMessage message = notificationService.messages.getFirst();
      assertThat(message.userId()).isEqualTo(REVIEW_AUTHOR_ID);
      assertThat(message.type()).isEqualTo(NotificationType.USER);
      assertThat(message.category()).isEqualTo(NotificationCategory.REVIEW);
      assertThat(message.title()).isEqualTo(ReviewReplyNotificationListener.TITLE);
      assertThat(message.content()).isEqualTo(content);
      assertThat(message.sourceType()).isEqualTo(NotificationSourceType.REVIEW_REPLY);
      assertThat(message.sourceId()).isEqualTo(REPLY_ID);
      assertThat(message.action().type()).isEqualTo(NotificationActionType.OPEN_REVIEW);
      assertThat(message.action().targetId()).isEqualTo(REVIEW_ID);
      assertThat(message.action().payload().path("replyId").longValue()).isEqualTo(REPLY_ID);
      assertThat(message.action().version()).isEqualTo(NotificationAction.CURRENT_VERSION);
    }

    @Test
    @DisplayName("리뷰 작성자 본인 댓글이면 알림을 보내지 않는다")
    void handleReviewReplyNotification_whenSelfReply_skipsNotification() {
      // given
      ReviewReplyActivityEvent event =
          new ReviewReplyActivityEvent(
              REVIEW_ID, 20L, REVIEW_AUTHOR_ID, REVIEW_AUTHOR_ID, REPLY_ID, null, "내 리뷰에 댓글");

      // when
      listener.handleReviewReplyNotification(event);

      // then
      assertThat(notificationService.messages).isEmpty();
    }

    @Test
    @DisplayName("이벤트가 null이면 아무 것도 하지 않는다")
    void handleReviewReplyNotification_whenEventNull_doesNothing() {
      // when
      listener.handleReviewReplyNotification(null);

      // then
      assertThat(notificationService.messages).isEmpty();
    }
  }

  @Nested
  @DisplayName("대댓글 수신자를 결정할 때")
  class ReplyRecipients {
    @Test
    @DisplayName("리뷰 작성자와 직접 부모 댓글 작성자에게 각각 전달한다")
    void distinctRecipients() {
      listener.handleReviewReplyNotification(reply(1L, 2L, 3L));

      assertThat(notificationService.messages).extracting(NotificationMessage::userId).containsExactly(1L, 2L);
      assertThat(notificationService.messages).extracting(NotificationMessage::title)
          .containsExactly("새 댓글", "새 답글");
    }

    @Test
    @DisplayName("리뷰 작성자가 답글을 달 때 부모 댓글 작성자에게만 전달한다")
    void reviewAuthorReplies() {
      listener.handleReviewReplyNotification(reply(1L, 2L, 1L));

      assertThat(notificationService.messages).extracting(NotificationMessage::userId).containsExactly(2L);
    }

    @Test
    @DisplayName("부모 댓글 작성자가 답글을 달 때 리뷰 작성자에게만 전달한다")
    void parentAuthorReplies() {
      listener.handleReviewReplyNotification(reply(1L, 2L, 2L));

      assertThat(notificationService.messages).extracting(NotificationMessage::userId).containsExactly(1L);
    }

    @Test
    @DisplayName("리뷰와 부모 댓글 작성자가 같을 때 부모 답글 한 건만 전달한다")
    void sameRecipient() {
      listener.handleReviewReplyNotification(reply(1L, 1L, 2L));

      assertThat(notificationService.messages).hasSize(1);
      assertThat(notificationService.messages.getFirst().title()).isEqualTo("새 답글");
    }

    @Test
    @DisplayName("모든 작성자가 본인일 때 전달하지 않는다")
    void allSelf() {
      listener.handleReviewReplyNotification(reply(1L, 1L, 1L));

      assertThat(notificationService.messages).isEmpty();
    }

    @Test
    @DisplayName("리뷰 작성자 저장이 실패해도 부모 댓글 작성자에게 전달한다")
    void failureDoesNotBlockOtherRecipient() {
      notificationService.failingUserId = 1L;

      assertThatThrownBy(() -> listener.handleReviewReplyNotification(reply(1L, 2L, 3L)))
          .isInstanceOf(IllegalStateException.class)
          .satisfies(failure -> assertThat(failure.getSuppressed()).hasSize(1));

      assertThat(notificationService.messages).extracting(NotificationMessage::userId).containsExactly(2L);
    }

    @Test
    @DisplayName("부모 댓글 작성자 저장이 실패해도 리뷰 작성자에게 전달한다")
    void parentFailureDoesNotUndoReviewNotification() {
      notificationService.failingUserId = 2L;

      assertThatThrownBy(() -> listener.handleReviewReplyNotification(reply(1L, 2L, 3L)))
          .isInstanceOf(IllegalStateException.class)
          .satisfies(failure -> assertThat(failure.getSuppressed()).hasSize(1));

      assertThat(notificationService.messages).extracting(NotificationMessage::userId).containsExactly(1L);
    }
  }

  @Nested
  @DisplayName("좋아요와 팔로우를 구독할 때")
  class OtherActivities {
    @Test
    @DisplayName("다른 사용자의 좋아요 활성 전이만 리뷰 작성자에게 전달한다")
    void likes() {
      var likes = new ReviewLikeNotificationListener(notificationService);
      likes.handle(new ReviewLikeActivityEvent(10L, 20L, 30L, 1L, 2L, "리뷰", true));
      likes.handle(new ReviewLikeActivityEvent(10L, 20L, 30L, 1L, 2L, "리뷰", false));
      likes.handle(new ReviewLikeActivityEvent(11L, 20L, 30L, 1L, 1L, "리뷰", true));

      assertThat(notificationService.messages).hasSize(1);
      NotificationMessage message = notificationService.messages.getFirst();
      assertThat(message.sourceType()).isEqualTo(NotificationSourceType.REVIEW_LIKE);
      assertThat(message.userId()).isEqualTo(1L);
      assertThat(message.sourceId()).isEqualTo(10L);
      assertThat(message.action().targetId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("자기 자신을 제외한 팔로우 대상에게 행위자 식별자를 전달한다")
    void follows() {
      var follows = new FollowNotificationListener(notificationService);
      follows.handle(new FollowActivityEvent(10L, 1L, 2L));
      follows.handle(new FollowActivityEvent(11L, 1L, 1L));

      assertThat(notificationService.messages).hasSize(1);
      NotificationMessage message = notificationService.messages.getFirst();
      assertThat(message.sourceType()).isEqualTo(NotificationSourceType.FOLLOW);
      assertThat(message.userId()).isEqualTo(2L);
      assertThat(message.sourceId()).isEqualTo(10L);
      assertThat(message.action().targetId()).isEqualTo(1L);
    }
  }

  private ReviewReplyActivityEvent reply(Long reviewAuthor, Long parentAuthor, Long actor) {
    return new ReviewReplyActivityEvent(REVIEW_ID, 20L, reviewAuthor, actor, REPLY_ID, parentAuthor, "답글");
  }

  /** NotificationService 호출 기록용 테스트 더블. */
  private static final class RecordingNotificationService implements NotificationService {
    private final List<NotificationMessage> messages = new ArrayList<>();
    private Long failingUserId;

    @Override
    public void sendNotification(NotificationMessage message) {
      if (message.userId().equals(failingUserId)) {
        throw new IllegalStateException("수신자 저장 실패");
      }
      messages.add(message);
    }

    @Override
    public KeysetPageResponse<NotificationListResponse> getNotifications(
        Long userId, NotificationPageableRequest request) {
      return KeysetPageResponse.of(
          NotificationListResponse.of(List.of()), new KeysetPagination(false, null));
    }

    @Override
    public long countUnread(Long userId) {
      return 0;
    }

    @Override
    public NotificationMarkReadResult markAsRead(Long userId, Long notificationId) {
      return new NotificationMarkReadResult(notificationId, true, null, false, 0);
    }

    @Override
    public int markAllAsRead(Long userId) {
      return 0;
    }
  }
}
