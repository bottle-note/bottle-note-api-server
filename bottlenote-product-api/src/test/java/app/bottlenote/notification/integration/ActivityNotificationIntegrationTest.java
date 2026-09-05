package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.service.LikesCommandService;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.review.service.ReviewReplyService;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.FollowRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.fixture.UserTestFactory;
import app.bottlenote.user.service.FollowService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@DisplayName("활동 이벤트와 알림 및 History 통합 테스트")
class ActivityNotificationIntegrationTest extends IntegrationTestSupport {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final Duration HOLD = Duration.ofSeconds(1);

  @Autowired private UserTestFactory users;
  @Autowired private AlcoholTestFactory alcohols;
  @Autowired private ReviewTestFactory reviews;
  @Autowired private ReviewReplyService replyService;
  @Autowired private LikesCommandService likesService;
  @Autowired private FollowService followService;
  @Autowired private ReviewReplyRepository replies;
  @Autowired private LikesRepository likes;
  @Autowired private FollowRepository follows;
  @Autowired private NotificationRepository notifications;
  @Autowired private UserHistoryRepository histories;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private PlatformTransactionManager transactionManager;

  private TransactionTemplate transaction;
  private User reviewAuthor;
  private User parentAuthor;
  private User actor;
  private Review review;

  @BeforeEach
  void setUp() {
    transaction = new TransactionTemplate(transactionManager);
    reviewAuthor = users.persistUser();
    parentAuthor = users.persistUser();
    actor = users.persistUser();
    review = reviews.persistReview(reviewAuthor, alcohols.persistAlcohol());
  }

  @Nested
  @DisplayName("댓글 활동을 처리할 때")
  class Replies {
    @Test
    @DisplayName("대댓글을 등록하면 두 수신자에게 알리고 History 한 건의 기존 내용을 보존한다")
    void reply_deliversToBothAndPreservesHistory() {
      ReviewReply parent = reviews.persistReviewReply(review, parentAuthor);

      replyService.registerReviewReply(
          review.getId(), actor.getId(), new ReviewReplyRegisterRequest("대댓글 내용", parent.getId()));

      awaitCounts(2, 1);
      assertThat(messages(reviewAuthor))
          .singleElement()
          .satisfies(
              message -> {
                assertThat(message.getTitle()).isEqualTo("새 댓글");
                assertThat(message.getContent()).isEqualTo("대댓글 내용");
                assertThat(message.getActionTargetId()).isEqualTo(review.getId());
              });
      assertThat(messages(parentAuthor))
          .singleElement()
          .satisfies(
              message -> {
                assertThat(message.getTitle()).isEqualTo("새 답글");
                assertThat(message.getSourceId())
                    .isEqualTo(messages(reviewAuthor).getFirst().getSourceId());
              });
      assertThat(messages(actor)).isEmpty();
      assertHistory(histories.findAll().getFirst(), actor, EventType.REVIEW_REPLY_CREATE, "대댓글 내용");
    }

    @Test
    @DisplayName("중첩 답글을 등록하면 최상위 댓글 대신 직접 부모 댓글 작성자에게 알린다")
    void nestedReply_targetsDirectParent() {
      User rootAuthor = users.persistUser();
      ReviewReply root = reviews.persistReviewReply(review, rootAuthor);
      ReviewReply parent = reviews.persistReviewReply(review, parentAuthor, root, "부모 댓글");

      replyService.registerReviewReply(
          review.getId(), actor.getId(), new ReviewReplyRegisterRequest("중첩 답글", parent.getId()));

      awaitCounts(2, 1);
      assertThat(messages(parentAuthor)).hasSize(1);
      assertThat(messages(reviewAuthor)).hasSize(1);
      assertThat(messages(rootAuthor)).isEmpty();
    }

    @Test
    @DisplayName("리뷰 작성자가 답글을 달면 직접 부모 작성자만 수신한다")
    void reviewAuthorReply_excludesSelf() {
      ReviewReply parent = reviews.persistReviewReply(review, parentAuthor);

      replyService.registerReviewReply(
          review.getId(),
          reviewAuthor.getId(),
          new ReviewReplyRegisterRequest("작성자 답글", parent.getId()));

      awaitCounts(1, 1);
      assertThat(messages(reviewAuthor)).isEmpty();
      assertThat(messages(parentAuthor)).hasSize(1);
    }

    @Test
    @DisplayName("리뷰와 부모 댓글 작성자가 같으면 답글 알림 한 건을 받는다")
    void sameRecipient_receivesOneReply() {
      ReviewReply parent = reviews.persistReviewReply(review, reviewAuthor);

      replyService.registerReviewReply(
          review.getId(),
          actor.getId(),
          new ReviewReplyRegisterRequest("부모 답글 경로", parent.getId()));

      awaitCounts(1, 1);
      assertThat(messages(reviewAuthor))
          .singleElement()
          .satisfies(message -> assertThat(message.getTitle()).isEqualTo("새 답글"));
    }

    @Test
    @DisplayName("댓글 원본 트랜잭션이 rollback되면 댓글과 알림과 History가 남지 않는다")
    void rollbackReply_hasNoSideEffects() {
      transaction.executeWithoutResult(
          status -> {
            replyService.registerReviewReply(
                review.getId(), actor.getId(), new ReviewReplyRegisterRequest("취소할 댓글", null));
            status.setRollbackOnly();
          });

      awaitCounts(0, 0);
      assertThat(replies.findAllReply()).isEmpty();
    }
  }

  @Nested
  @DisplayName("좋아요와 팔로우 상태가 바뀔 때")
  class Relationships {
    @Test
    @DisplayName("좋아요 반복과 취소와 재활성화는 알림 한 건을 유지하고 History는 요청마다 남긴다")
    void likeTransitions_keepFirstNotificationAndAllHistory() {
      for (LikeStatus status :
          List.of(LikeStatus.LIKE, LikeStatus.LIKE, LikeStatus.DISLIKE, LikeStatus.LIKE)) {
        likesService.updateLikes(actor.getId(), review.getId(), status);
      }

      awaitCounts(1, 4);
      assertThat(messages(reviewAuthor))
          .singleElement()
          .satisfies(
              message -> {
                assertThat(message.getSourceType()).isEqualTo("REVIEW_LIKE");
                assertThat(message.getActionTargetId()).isEqualTo(review.getId());
              });
      assertThat(histories.findAll())
          .allSatisfy(
              history ->
                  assertHistory(history, actor, EventType.REVIEW_LIKES, review.getContent()));
    }

    @Test
    @DisplayName("본인 리뷰에 좋아요를 누르면 History만 기록한다")
    void selfLike_hasOnlyHistory() {
      likesService.updateLikes(reviewAuthor.getId(), review.getId(), LikeStatus.LIKE);

      awaitCounts(0, 1);
      assertHistory(
          histories.findAll().getFirst(),
          reviewAuthor,
          EventType.REVIEW_LIKES,
          review.getContent());
    }

    @Test
    @DisplayName("팔로우 반복과 취소와 재활성화는 대상 사용자에게 최초 관계 알림만 남긴다")
    void followTransitions_keepFirstNotification() {
      for (FollowStatus status :
          List.of(
              FollowStatus.FOLLOWING,
              FollowStatus.FOLLOWING,
              FollowStatus.UNFOLLOW,
              FollowStatus.FOLLOWING)) {
        followService.updateFollowStatus(
            new FollowUpdateRequest(reviewAuthor.getId(), status), actor.getId());
      }

      awaitCounts(1, 0);
      assertThat(messages(reviewAuthor))
          .singleElement()
          .satisfies(
              message -> {
                assertThat(message.getSourceType()).isEqualTo("FOLLOW");
                assertThat(message.getActionType()).isEqualTo("OPEN_USER");
                assertThat(message.getActionTargetId()).isEqualTo(actor.getId());
              });
      assertThat(messages(actor)).isEmpty();
    }

    @Test
    @DisplayName("좋아요와 팔로우 원본이 rollback되면 관계와 알림과 History가 남지 않는다")
    void rollbackRelationships_hasNoSideEffects() {
      transaction.executeWithoutResult(
          status -> {
            likesService.updateLikes(actor.getId(), review.getId(), LikeStatus.LIKE);
            followService.updateFollowStatus(
                new FollowUpdateRequest(reviewAuthor.getId(), FollowStatus.FOLLOWING),
                actor.getId());
            status.setRollbackOnly();
          });

      awaitCounts(0, 0);
      assertThat(likes.findByReviewIdAndUserId(review.getId(), actor.getId())).isEmpty();
      assertThat(follows.findByUserIdAndFollowUserId(actor.getId(), reviewAuthor.getId()))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("같은 활동을 다시 전달하거나 수신자가 실패할 때")
  class Delivery {
    @Test
    @DisplayName("동시 재전달과 이미 저장한 한 수신자의 중복은 다른 수신자 저장을 막지 않는다")
    void concurrentReplay_convergesPerRecipient() throws Exception {
      ReviewReply source = reviews.persistReviewReply(review, actor);
      ReviewReplyActivityEvent initial = activity(source, reviewAuthor.getId(), null);
      transaction.executeWithoutResult(status -> events.publishEvent(initial));
      awaitCounts(1, 1);
      ReviewReplyActivityEvent replay =
          activity(source, reviewAuthor.getId(), parentAuthor.getId());
      int deliveries = 8;
      CountDownLatch ready = new CountDownLatch(deliveries);
      CountDownLatch start = new CountDownLatch(1);
      List<Future<?>> futures = new ArrayList<>();
      try (var executor = Executors.newFixedThreadPool(deliveries)) {
        for (int index = 0; index < deliveries; index++) {
          futures.add(
              executor.submit(
                  () -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                      throw new IllegalStateException("재전달 시작 시간 초과");
                    }
                    transaction.executeWithoutResult(status -> events.publishEvent(replay));
                    return null;
                  }));
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
          future.get(10, TimeUnit.SECONDS);
        }
      } finally {
        start.countDown();
      }

      awaitCounts(2, deliveries + 1);
      assertThat(messages(reviewAuthor)).hasSize(1);
      assertThat(messages(parentAuthor)).hasSize(1);
    }

    @Test
    @DisplayName("한 수신자가 사라져 알림 처리가 실패해도 원본과 다른 수신자의 알림은 남는다")
    void failedRecipient_doesNotRollbackSourceOrOtherRecipient() {
      Long sourceId =
          transaction.execute(
              status -> {
                ReviewReply source = reviews.persistReviewReply(review, actor);
                events.publishEvent(activity(source, Long.MAX_VALUE, parentAuthor.getId()));
                return source.getId();
              });

      awaitCounts(1, 1);
      assertThat(replies.findReplyById(sourceId)).isPresent();
      assertThat(messages(parentAuthor)).hasSize(1);
    }
  }

  private ReviewReplyActivityEvent activity(ReviewReply source, Long authorId, Long parentId) {
    return new ReviewReplyActivityEvent(
        review.getId(),
        review.getAlcoholId(),
        authorId,
        actor.getId(),
        source.getId(),
        parentId,
        source.getContent());
  }

  private List<Notification> messages(User user) {
    return notifications.findPageByUserId(NotificationListCriteria.of(user.getId(), 0L, 100L));
  }

  private void awaitCounts(long notificationCount, int historyCount) {
    await()
        .atMost(TIMEOUT)
        .during(HOLD)
        .untilAsserted(
            () -> {
              assertThat(
                      notifications.countByUserId(reviewAuthor.getId())
                          + notifications.countByUserId(parentAuthor.getId())
                          + notifications.countByUserId(actor.getId()))
                  .isEqualTo(notificationCount);
              assertThat(histories.findAll()).hasSize(historyCount);
            });
  }

  private void assertHistory(UserHistory history, User user, EventType type, String content) {
    assertThat(history.getUserId()).isEqualTo(user.getId());
    assertThat(history.getAlcoholId()).isEqualTo(review.getAlcoholId());
    assertThat(history.getEventCategory()).isEqualTo(EventCategory.REVIEW);
    assertThat(history.getEventType()).isEqualTo(type);
    assertThat(history.getContent()).isEqualTo(content);
    assertThat(history.getRedirectUrl())
        .isEqualTo(RedirectUrlType.REVIEW.getUrl() + "/" + review.getId());
  }
}
