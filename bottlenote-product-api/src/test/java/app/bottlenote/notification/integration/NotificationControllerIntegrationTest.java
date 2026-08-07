package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] NotificationController")
class NotificationControllerIntegrationTest extends IntegrationTestSupport {

  private static final String BASE = "/api/v1/notifications";

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private NotificationRepository notificationRepository;

  @Nested
  @DisplayName("알림 목록 조회")
  class GetNotifications {

    @Test
    @DisplayName("인증 사용자는 본인 알림만 최신순으로 조회한다")
    void getNotifications_whenAuthenticated_returnsOwnNotificationsOrderedByIdDesc()
        throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      Notification older = seedNotification(user.getId(), "old-title");
      Notification newer = seedNotification(user.getId(), "new-title");
      seedNotification(other.getId(), "other-title");

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(fieldNames(data)).containsExactlyInAnyOrder("totalCount", "items");
      assertThat(data.path("totalCount").asLong()).isEqualTo(2);
      assertThat(data.path("items")).hasSize(2);
      assertThat(data.path("items").get(0).path("id").asLong()).isEqualTo(newer.getId());
      assertThat(data.path("items").get(0).path("title").asText()).isEqualTo("new-title");
      assertThat(data.path("items").get(1).path("id").asLong()).isEqualTo(older.getId());
      assertThat(fieldNames(data.path("items").get(0)))
          .containsExactlyInAnyOrder(
              "id", "title", "content", "type", "category", "status", "isRead", "createAt");
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getNotifications_whenUnauthenticated_returnsUnauthorized() {
      mockMvcTester.get().uri(BASE).exchange().assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("미읽음 개수 조회")
  class GetUnreadCount {

    @Test
    @DisplayName("인증 사용자는 본인 미읽음 개수만 조회한다")
    void getUnreadCount_whenAuthenticated_returnsOwnUnreadCount() throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      Notification read = seedNotification(user.getId(), "read");
      read.markAsRead();
      notificationRepository.save(read);
      seedNotification(user.getId(), "unread-1");
      seedNotification(user.getId(), "unread-2");
      seedNotification(other.getId(), "other-unread");

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE + "/unread-count")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(fieldNames(data)).containsExactly("unreadCount");
      assertThat(data.path("unreadCount").asLong()).isEqualTo(2);
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getUnreadCount_whenUnauthenticated_returnsUnauthorized() {
      mockMvcTester
          .get()
          .uri(BASE + "/unread-count")
          .exchange()
          .assertThat()
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("단건 읽음 처리")
  class MarkAsRead {

    @Test
    @DisplayName("본인 알림을 읽음 처리한다")
    void markAsRead_whenOwnNotification_marksRead() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification notification = seedNotification(user.getId(), "target");

      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri(BASE + "/{notificationId}/read", notification.getId())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(data.path("notificationId").asLong()).isEqualTo(notification.getId());
      assertThat(data.path("isRead").asBoolean()).isTrue();

      Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
      assertThat(reloaded.getIsRead()).isTrue();
      assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("타 사용자 알림은 404를 반환한다")
    void markAsRead_whenOtherUsersNotification_returnsNotFound() throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification otherNotification = seedNotification(other.getId(), "other");

      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri(BASE + "/{notificationId}/read", otherNotification.getId())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatus(HttpStatus.NOT_FOUND);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(NotificationExceptionCode.NOTIFICATION_NOT_FOUND.name());

      Notification reloaded =
          notificationRepository.findById(otherNotification.getId()).orElseThrow();
      assertThat(reloaded.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void markAsRead_whenUnauthenticated_returnsUnauthorized() {
      mockMvcTester
          .patch()
          .uri(BASE + "/{notificationId}/read", 1L)
          .with(csrf())
          .exchange()
          .assertThat()
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("전체 읽음 처리")
  class MarkAllAsRead {

    @Test
    @DisplayName("본인 미읽음 알림만 모두 읽음 처리한다")
    void markAllAsRead_whenAuthenticated_marksOnlyOwnUnread() throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      seedNotification(user.getId(), "unread-1");
      seedNotification(user.getId(), "unread-2");
      Notification otherNotification = seedNotification(other.getId(), "other-unread");

      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri(BASE + "/read-all")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(data.path("updatedCount").asInt()).isEqualTo(2);

      assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
      assertThat(
              notificationRepository.findById(otherNotification.getId()).orElseThrow().getIsRead())
          .isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void markAllAsRead_whenUnauthenticated_returnsUnauthorized() {
      mockMvcTester
          .patch()
          .uri(BASE + "/read-all")
          .with(csrf())
          .exchange()
          .assertThat()
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  private Notification seedNotification(Long userId, String title) {
    return notificationRepository.save(
        Notification.builder()
            .userId(userId)
            .title(title)
            .content(title + "-content")
            .type(NotificationType.USER)
            .category(NotificationCategory.REVIEW)
            .status(NotificationStatus.PENDING)
            .isRead(false)
            .build());
  }

  private JsonNode responseData(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private Set<String> fieldNames(JsonNode node) {
    return StreamSupport.stream(((Iterable<String>) () -> node.fieldNames()).spliterator(), false)
        .collect(java.util.stream.Collectors.toSet());
  }
}
