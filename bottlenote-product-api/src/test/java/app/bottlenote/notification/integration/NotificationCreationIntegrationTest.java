package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@DisplayName("[integration] Notification 공통 생성 경로")
class NotificationCreationIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationService notificationService;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("새 리뷰와 팔로우 알림을 조회하면 버전별 빈 객체 Action을 반환한다")
  void newActions_areSerializedInNotificationList() throws Exception {
    User user = userTestFactory.persistUser();
    var token = getToken(user);
    notificationService.sendNotification(
        NotificationMessage.reviewLike(user.getId(), 30L, 400L, "좋아요", "내용"));
    notificationService.sendNotification(
        NotificationMessage.follow(user.getId(), 20L, 300L, "팔로워", "내용"));

    var result =
        mockMvcTester
            .get()
            .uri("/api/v1/notifications")
            .header("Authorization", "Bearer " + token.accessToken())
            .exchange();

    result.assertThat().hasStatusOk();
    var items =
        mapper.readTree(result.getResponse().getContentAsByteArray()).path("data").path("items");
    assertThat(items.size()).isEqualTo(2);
    var follow = items.get(0);
    assertThat(follow.path("category").asText()).isEqualTo("FOLLOW");
    assertThat(follow.path("action").path("type").asText()).isEqualTo("OPEN_USER");
    assertThat(follow.path("action").path("targetId").asLong()).isEqualTo(20L);
    assertThat(follow.path("action").path("version").asInt()).isEqualTo(1);
    var review = items.get(1);
    assertThat(review.path("action").path("type").asText()).isEqualTo("OPEN_REVIEW");
    assertThat(review.path("action").path("targetId").asLong()).isEqualTo(30L);
    assertThat(review.path("action").path("version").asInt()).isEqualTo(2);
    for (var item : items) {
      assertThat(item.path("action").path("payload").isObject()).isTrue();
      assertThat(item.path("action").path("payload").isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("같은 원본을 저장할 때")
  class SaveIdempotently {

    @Test
    @DisplayName("동시에 중복 전송해도 모두 정상 종료하고 기존 한 건을 변경하지 않는다")
    void sendNotification_whenSameSourceConcurrent_preservesFirstStoredRow() throws Exception {
      User user = userTestFactory.persistUser();
      LocalDateTime originalReadAt = LocalDateTime.of(2026, 9, 5, 12, 34, 56);
      NotificationMessage first =
          NotificationMessage.reviewLike(user.getId(), 30L, 400L, "최초 제목", "최초 본문");

      LocalDateTime before = LocalDateTime.now().minusSeconds(1);
      sendConcurrently(first, 8);

      assertThat(notificationRepository.countByUserId(user.getId())).isEqualTo(1L);
      Long notificationId =
          jdbcTemplate.queryForObject(
              "SELECT id FROM notifications WHERE user_id = ?", Long.class, user.getId());
      StoredNotification original = storedNotification(notificationId);
      assertThat(original.createdAt()).isBetween(before, LocalDateTime.now().plusSeconds(1));
      assertThat(original.modifiedAt()).isEqualTo(original.createdAt());
      jdbcTemplate.update(
          "UPDATE notifications SET status = ?, is_read = ?, read_at = ? WHERE id = ?",
          NotificationStatus.SENT.name(),
          true,
          originalReadAt,
          notificationId);

      NotificationMessage duplicate =
          NotificationMessage.reviewLike(user.getId(), 30L, 400L, "중복 제목", "중복 전송 본문");
      sendConcurrently(duplicate, 8);

      assertThat(notificationRepository.countByUserId(user.getId())).isEqualTo(1L);
      StoredNotification stored = storedNotification(notificationId);
      assertThat(stored.title()).isEqualTo("최초 제목");
      assertThat(stored.content()).isEqualTo("최초 본문");
      assertThat(stored.createdAt()).isEqualTo(original.createdAt());
      assertThat(stored.modifiedAt()).isEqualTo(original.modifiedAt());
      assertThat(stored.status()).isEqualTo(NotificationStatus.SENT.name());
      assertThat(stored.read()).isTrue();
      assertThat(stored.readAt()).isEqualTo(originalReadAt);
    }
  }

  @Nested
  @DisplayName("호출 트랜잭션과 분리할 때")
  class SeparateTransaction {

    @Test
    @DisplayName("호출 트랜잭션이 rollback되어도 이미 완료한 알림 저장은 유지한다")
    void sendNotification_whenOuterTransactionRollsBack_keepsRequiresNewCommit() {
      User user = userTestFactory.persistUser();
      TransactionTemplate outer = new TransactionTemplate(transactionManager);

      outer.executeWithoutResult(
          status -> {
            notificationService.sendNotification(
                NotificationMessage.reviewLike(user.getId(), 50L, 500L, "새 좋아요", "좋아요가 등록됐습니다."));
            status.setRollbackOnly();
          });

      assertThat(notificationRepository.countByUserId(user.getId())).isEqualTo(1L);
    }
  }

  private void sendAfterStart(
      NotificationMessage message, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 시작 신호를 받지 못했습니다.");
      }
      notificationService.sendNotification(message);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private void sendConcurrently(NotificationMessage message, int requestCount) throws Exception {
    CountDownLatch ready = new CountDownLatch(requestCount);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requestCount);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < requestCount; i++) {
        futures.add(executor.submit(() -> sendAfterStart(message, ready, start)));
      }

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  private StoredNotification storedNotification(Long notificationId) {
    return jdbcTemplate.queryForObject(
        "SELECT title, content, status, is_read, read_at, create_at, last_modify_at FROM notifications WHERE id = ?",
        (resultSet, rowNumber) ->
            new StoredNotification(
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("status"),
                resultSet.getBoolean("is_read"),
                resultSet.getTimestamp("read_at") == null
                    ? null
                    : resultSet.getTimestamp("read_at").toLocalDateTime(),
                resultSet.getTimestamp("create_at").toLocalDateTime(),
                resultSet.getTimestamp("last_modify_at").toLocalDateTime()),
        notificationId);
  }

  private record StoredNotification(
      String title,
      String content,
      String status,
      boolean read,
      LocalDateTime readAt,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt) {}
}
