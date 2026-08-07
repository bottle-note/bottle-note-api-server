package app.bottlenote.notification.event.listener;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.event.payload.ReviewReplyNotificationEvent;
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
      ReviewReplyNotificationEvent event =
          ReviewReplyNotificationEvent.of(
              REVIEW_ID, REVIEW_AUTHOR_ID, OTHER_USER_ID, REPLY_ID, content);

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
    }

    @Test
    @DisplayName("리뷰 작성자 본인 댓글이면 알림을 보내지 않는다")
    void handleReviewReplyNotification_whenSelfReply_skipsNotification() {
      // given
      ReviewReplyNotificationEvent event =
          ReviewReplyNotificationEvent.of(
              REVIEW_ID, REVIEW_AUTHOR_ID, REVIEW_AUTHOR_ID, REPLY_ID, "내 리뷰에 댓글");

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

  /** NotificationService 호출 기록용 테스트 더블. */
  private static final class RecordingNotificationService implements NotificationService {
    private final List<NotificationMessage> messages = new ArrayList<>();

    @Override
    public void sendNotification(NotificationMessage message) {
      messages.add(message);
    }

    @Override
    public List<Notification> getNotifications(Long userId) {
      return List.of();
    }

    @Override
    public long countUnread(Long userId) {
      return 0;
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
      // no-op
    }

    @Override
    public int markAllAsRead(Long userId) {
      return 0;
    }
  }
}
