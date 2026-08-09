package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.notification.action.NotificationAction.OpenReviewActionPayload;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.repository.JpaNotificationRepository;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SC1: 댓글 등록 트랜잭션 commit 후 {@code ReviewReplyNotificationEvent}가 AFTER_COMMIT 비동기 listener를 거쳐
 * notifications 행으로 저장되는지 검증한다. Mockito/no-op publisher 없이 실제 Spring 이벤트 경로를 탄다.
 */
@Tag("integration")
@DisplayName("[integration] ReviewReply → Notification (AFTER_COMMIT async)")
class ReviewReplyNotificationIntegrationTest extends IntegrationTestSupport {

  private static final String REGISTER_URI = "/api/v1/review/reply/register/{reviewId}";
  private static final String EXPECTED_TITLE = "새 댓글";
  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration SELF_REPLY_HOLD = Duration.ofSeconds(2);

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private JpaNotificationRepository jpaNotificationRepository;
  @Autowired private NotificationService notificationService;
  @Autowired private PlatformTransactionManager transactionManager;

  @Nested
  @DisplayName("다른 사용자가 댓글을 등록할 때")
  class WhenOtherUserReplies {

    @Test
    @DisplayName("commit 후 리뷰 작성자 notifications 행이 저장된다")
    void registerReply_whenOtherUser_savesNotificationForReviewAuthor() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      TokenItem replyToken = getToken(replyAuthor);
      String content = "좋은 리뷰네요! SC1 통합 검증";

      // when — 요청 종료 시 댓글 트랜잭션이 commit되고 AFTER_COMMIT async listener가 동작한다
      MvcTestResult result =
          mockMvcTester
              .post()
              .uri(REGISTER_URI, review.getId())
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(new ReviewReplyRegisterRequest(content, null)))
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + replyToken.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();

      // then — bounded await로 async 저장 완료를 기다린다
      await()
          .atMost(ASYNC_TIMEOUT)
          .pollInterval(100, TimeUnit.MILLISECONDS)
          .untilAsserted(
              () -> {
                List<Notification> notifications =
                    notificationRepository.findPageByUserId(
                        NotificationListCriteria.of(reviewAuthor.getId(), 0L, 10L));
                assertThat(notifications).hasSize(1);
                Notification notification = notifications.getFirst();
                assertThat(notification.getUserId()).isEqualTo(reviewAuthor.getId());
                assertThat(notification.getTitle()).isEqualTo(EXPECTED_TITLE);
                assertThat(notification.getContent()).isEqualTo(content);
                assertThat(notification.getType()).isEqualTo(NotificationType.USER);
                assertThat(notification.getCategory()).isEqualTo(NotificationCategory.REVIEW);
                assertThat(notification.getIsRead()).isFalse();
                assertThat(notification.getSourceType())
                    .isEqualTo(NotificationSourceType.REVIEW_REPLY.name());
                assertThat(notification.getSourceId()).isPositive();
                assertThat(notification.getActionType()).isEqualTo("OPEN_REVIEW");
                assertThat(notification.getActionTargetId()).isEqualTo(review.getId());
                assertThat(notification.getActionPayload().path("replyId").longValue())
                    .isEqualTo(notification.getSourceId());
                assertThat(notification.getActionVersion()).isEqualTo((short) 1);

                var item =
                    notificationService
                        .getNotifications(
                            reviewAuthor.getId(), NotificationPageableRequest.builder().build())
                        .content()
                        .items()
                        .getFirst();
                assertThat(item.action().type().name()).isEqualTo("OPEN_REVIEW");
                assertThat(item.action().targetId()).isEqualTo(review.getId());
                assertThat(item.action().payload())
                    .isInstanceOfSatisfying(
                        OpenReviewActionPayload.class,
                        payload ->
                            assertThat(payload.replyId()).isEqualTo(notification.getSourceId()));
                assertThat(item.action().version()).isEqualTo(1);
              });

      // 댓글 작성자 본인에게는 알림이 없어야 한다
      assertThat(notificationRepository.countByUserId(replyAuthor.getId())).isZero();
    }

    @Test
    @DisplayName("동일 source 알림을 순차 전송하면 한 건만 저장한다")
    void sendNotification_whenSameSourceSentSequentially_savesOnce() {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      NotificationMessage message =
          NotificationMessage.reviewReply(
              reviewAuthor.getId(), 101L, 201L, EXPECTED_TITLE, "순차 중복 방지");

      // when
      notificationService.sendNotification(message);
      notificationService.sendNotification(message);

      // then
      assertThat(notificationRepository.countByUserId(reviewAuthor.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("별도 트랜잭션 raw 중복 저장은 DB UNIQUE가 거부한다")
    void saveNotification_whenRawSourceDuplicated_rejectsByDatabaseUnique() {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      NotificationMessage message =
          NotificationMessage.reviewReply(
              reviewAuthor.getId(), 102L, 202L, EXPECTED_TITLE, "DB 중복 방지");
      notificationService.sendNotification(message);
      Notification duplicate =
          Notification.builder()
              .userId(reviewAuthor.getId())
              .title(EXPECTED_TITLE)
              .content("DB 중복 방지")
              .type(NotificationType.USER)
              .category(NotificationCategory.REVIEW)
              .sourceType(NotificationSourceType.REVIEW_REPLY.name())
              .sourceId(202L)
              .action(message.action())
              .build();
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

      // when & then
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> jpaNotificationRepository.saveAndFlush(duplicate)))
          .isInstanceOf(DataIntegrityViolationException.class);
      assertThat(notificationRepository.countByUserId(reviewAuthor.getId())).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("리뷰 작성자 본인이 댓글을 등록할 때")
  class WhenSelfReplies {

    @Test
    @DisplayName("notifications 행을 생성하지 않는다")
    void registerReply_whenSelfReply_doesNotSaveNotification() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      TokenItem authorToken = getToken(reviewAuthor);

      // when
      MvcTestResult result =
          mockMvcTester
              .post()
              .uri(REGISTER_URI, review.getId())
              .contentType(APPLICATION_JSON)
              .content(
                  mapper.writeValueAsString(new ReviewReplyRegisterRequest("내 리뷰에 본인 댓글", null)))
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();

      // then — async 경로가 돌 시간을 포함한 구간 동안 저장되지 않음을 유지 검증
      await()
          .during(SELF_REPLY_HOLD)
          .atMost(ASYNC_TIMEOUT)
          .pollInterval(100, TimeUnit.MILLISECONDS)
          .untilAsserted(
              () ->
                  assertThat(notificationRepository.countByUserId(reviewAuthor.getId())).isZero());
    }
  }
}
