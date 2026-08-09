package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.notification.action.NotificationAction;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
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
  @Autowired private EntityManager entityManager;

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
              "id",
              "title",
              "content",
              "type",
              "category",
              "status",
              "isRead",
              "createAt",
              "readAt",
              "action");

      assertThat(data.path("items").get(0).path("action").isNull()).isTrue();
      assertThat(data.path("items").get(0).path("createAt").asText()).endsWith("+09:00");
      assertThat(data.path("items").get(0).path("readAt").isNull()).isTrue();

      JsonNode pageable = responseMeta(result).path("pageable");
      assertThat(pageable.path("currentCursor").asLong()).isZero();
      assertThat(pageable.path("pageSize").asLong()).isEqualTo(10L);
      assertThat(pageable.path("hasNext").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("id-desc keyset cursor로 다음 페이지를 조회한다")
    void getNotifications_whenKeysetCursor_returnsNextPageByLastItemId() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      Notification n1 = seedNotification(user.getId(), "n1");
      Notification n2 = seedNotification(user.getId(), "n2");
      Notification n3 = seedNotification(user.getId(), "n3");

      MvcTestResult first =
          mockMvcTester
              .get()
              .uri(BASE + "?cursor=0&pageSize=2")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      first.assertThat().hasStatusOk();
      JsonNode firstData = responseData(first);
      assertThat(firstData.path("totalCount").asLong()).isEqualTo(3);
      assertThat(firstData.path("items")).hasSize(2);
      assertThat(firstData.path("items").get(0).path("id").asLong()).isEqualTo(n3.getId());
      assertThat(firstData.path("items").get(1).path("id").asLong()).isEqualTo(n2.getId());
      JsonNode firstPageable = responseMeta(first).path("pageable");
      assertThat(firstPageable.path("hasNext").asBoolean()).isTrue();
      // nextCursor = 마지막 반환 item id
      assertThat(firstPageable.path("cursor").asLong()).isEqualTo(n2.getId());

      MvcTestResult second =
          mockMvcTester
              .get()
              .uri(BASE + "?cursor=" + n2.getId() + "&pageSize=2")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      second.assertThat().hasStatusOk();
      JsonNode secondData = responseData(second);
      assertThat(secondData.path("items")).hasSize(1);
      assertThat(secondData.path("items").get(0).path("id").asLong()).isEqualTo(n1.getId());
      assertThat(responseMeta(second).path("pageable").path("hasNext").asBoolean()).isFalse();
      assertThat(responseMeta(second).path("pageable").path("cursor").asLong())
          .isEqualTo(n1.getId());
    }

    @Test
    @DisplayName("타입 카테고리 읽음 상태를 결합해 본인 알림만 조회한다")
    void getNotifications_whenFiltersCombined_returnsMatchingOwnNotifications()
        throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification matching = seedNotification(user.getId(), "matching");
      Notification read = seedNotification(user.getId(), "read");
      read.markAsRead();
      notificationRepository.save(read);
      notificationRepository.save(
          Notification.builder()
              .userId(user.getId())
              .title("notice")
              .content("notice-content")
              .type(NotificationType.SYSTEM)
              .category(NotificationCategory.NOTICE)
              .build());
      seedNotification(other.getId(), "other");

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE + "?types=USER&categories=REVIEW&readStatus=UNREAD")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(data.path("totalCount").asLong()).isOne();
      assertThat(data.path("items")).hasSize(1);
      assertThat(data.path("items").get(0).path("id").asLong()).isEqualTo(matching.getId());
    }

    @Test
    @DisplayName("빈 타입과 카테고리 query는 전체 알림을 조회한다")
    void getNotifications_whenCollectionQueriesAreEmpty_returnsAllNotifications()
        throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      seedNotification(user.getId(), "review");
      notificationRepository.save(
          Notification.builder()
              .userId(user.getId())
              .title("notice")
              .content("notice-content")
              .type(NotificationType.SYSTEM)
              .category(NotificationCategory.NOTICE)
              .build());

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE + "?types=&categories=")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      assertThat(responseData(result).path("totalCount").asLong()).isEqualTo(2);
      assertThat(responseData(result).path("items")).hasSize(2);
    }

    @Test
    @DisplayName("동일 필터의 cursor 페이지는 중복과 누락 없이 id 내림차순이다")
    void getNotifications_whenFilteredCursorPagesRequested_returnsStableKeysetPages()
        throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification n1 = seedNotification(user.getId(), "n1");
      Notification n2 = seedNotification(user.getId(), "n2");
      Notification n3 = seedNotification(user.getId(), "n3");

      MvcTestResult first =
          mockMvcTester
              .get()
              .uri(BASE + "?types=USER&readStatus=ALL&pageSize=2")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();
      JsonNode firstData = responseData(first);
      long cursor = responseMeta(first).path("pageable").path("cursor").asLong();

      MvcTestResult second =
          mockMvcTester
              .get()
              .uri(BASE + "?types=USER&readStatus=ALL&pageSize=2&cursor=" + cursor)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();
      JsonNode secondData = responseData(second);

      first.assertThat().hasStatusOk();
      second.assertThat().hasStatusOk();
      assertThat(firstData.path("totalCount").asLong()).isEqualTo(3);
      assertThat(secondData.path("totalCount").asLong()).isEqualTo(3);
      assertThat(firstData.path("items").get(0).path("id").asLong()).isEqualTo(n3.getId());
      assertThat(firstData.path("items").get(1).path("id").asLong()).isEqualTo(n2.getId());
      assertThat(secondData.path("items").get(0).path("id").asLong()).isEqualTo(n1.getId());
    }

    @Test
    @DisplayName("생성 시각은 from 포함 to 제외로 조회한다")
    void getNotifications_whenCreatedRangeExists_usesHalfOpenInterval() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification from = seedNotification(user.getId(), "from");
      Notification inside = seedNotification(user.getId(), "inside");
      Notification to = seedNotification(user.getId(), "to");
      org.springframework.test.util.ReflectionTestUtils.setField(
          from, "createAt", LocalDateTime.of(2026, 8, 10, 9, 0));
      org.springframework.test.util.ReflectionTestUtils.setField(
          inside, "createAt", LocalDateTime.of(2026, 8, 10, 9, 30));
      org.springframework.test.util.ReflectionTestUtils.setField(
          to, "createAt", LocalDateTime.of(2026, 8, 10, 10, 0));
      notificationRepository.save(from);
      notificationRepository.save(inside);
      notificationRepository.save(to);

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(
                  BASE
                      + "?createdFrom=2026-08-10T00:00:00Z&createdTo=2026-08-10T01:00:00Z")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(data.path("totalCount").asLong()).isEqualTo(2);
      assertThat(data.path("items").get(0).path("id").asLong()).isEqualTo(inside.getId());
      assertThat(data.path("items").get(1).path("id").asLong()).isEqualTo(from.getId());
    }

    @Test
    @DisplayName("생성 시각 범위가 역전되거나 같으면 400을 반환한다")
    void getNotifications_whenCreatedRangeIsInvalid_returnsBadRequest() {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      MvcTestResult equalRange =
          mockMvcTester
              .get()
              .uri(BASE + "?createdFrom=2026-08-10T01:00:00Z&createdTo=2026-08-10T01:00:00Z")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();
      MvcTestResult reversedRange =
          mockMvcTester
              .get()
              .uri(BASE + "?createdFrom=2026-08-10T02:00:00Z&createdTo=2026-08-10T01:00:00Z")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      equalRange.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      reversedRange.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("OPEN_REVIEW Action을 의미 기반 계약으로 반환한다")
    void getNotifications_whenOpenReviewActionExists_returnsTypedAction() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification notification =
          Notification.builder()
              .userId(user.getId())
              .title("reply")
              .content("reply-content")
              .type(NotificationType.USER)
              .category(NotificationCategory.REVIEW)
              .action(NotificationAction.openReview(10L, 20L))
              .build();
      notification.markAsRead(java.time.LocalDateTime.of(2026, 8, 10, 12, 0));
      notificationRepository.save(notification);

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode item = responseData(result).path("items").get(0);
      assertThat(item.path("id").asLong()).isEqualTo(notification.getId());
      assertThat(item.path("action").path("type").asText()).isEqualTo("OPEN_REVIEW");
      assertThat(item.path("action").path("targetId").asLong()).isEqualTo(10L);
      assertThat(item.path("action").path("payload").path("replyId").asLong()).isEqualTo(20L);
      assertThat(item.path("action").path("version").asInt()).isEqualTo(1);
      assertThat(item.path("action").path("fallbackType").asText())
          .isEqualTo("OPEN_NOTIFICATION_CENTER");
      assertThat(item.path("createAt").asText()).endsWith("+09:00");
      assertThat(item.path("readAt").asText()).isEqualTo("2026-08-10T12:00:00+09:00");
    }

    @Test
    @DisplayName("유효하지 않은 raw Action은 해당 항목만 null로 강등한다")
    void getNotifications_whenRawActionsAreInvalid_returnsValidItemAndNullInvalidActions()
        throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification valid =
          seedActionNotification(
              user.getId(), "valid", NotificationAction.openReview(10L, 20L));
      Notification unknownType =
          seedRawActionNotification(
              user.getId(),
              "unknown-type",
              "OPEN_UNKNOWN",
              1,
              JsonNodeFactory.instance.objectNode().put("replyId", 21L));
      Notification unknownVersion =
          seedRawActionNotification(
              user.getId(),
              "unknown-version",
              "OPEN_REVIEW",
              2,
              JsonNodeFactory.instance.objectNode().put("replyId", 22L));
      Notification scalarPayload =
          seedRawActionNotification(
              user.getId(),
              "scalar",
              "OPEN_REVIEW",
              1,
              JsonNodeFactory.instance.textNode("invalid"));
      Notification incompletePayload =
          seedRawActionNotification(
              user.getId(),
              "incomplete",
              "OPEN_REVIEW",
              1,
              JsonNodeFactory.instance.objectNode());

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(BASE)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode items = responseData(result).path("items");
      assertThat(items).hasSize(5);
      assertThat(findItem(items, valid.getId()).path("action").path("type").asText())
          .isEqualTo("OPEN_REVIEW");
      assertThat(
              List.of(unknownType, unknownVersion, scalarPayload, incompletePayload).stream()
                  .map(notification -> findItem(items, notification.getId()).path("action"))
                  .toList())
          .allMatch(JsonNode::isNull);
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
    @DisplayName("본인 알림을 멱등하게 읽음 처리한다")
    void markAsRead_whenRepeated_returnsFirstReadAtAndChangedFalse() throws Exception {
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
      assertThat(data.path("readAt").asText()).endsWith("+09:00");
      assertThat(data.path("changed").asBoolean()).isTrue();
      assertThat(data.path("unreadCount").asLong()).isZero();

      MvcTestResult repeated =
          mockMvcTester
              .patch()
              .uri(BASE + "/{notificationId}/read", notification.getId())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();
      JsonNode repeatedData = responseData(repeated);
      repeated.assertThat().hasStatusOk();
      assertThat(repeatedData.path("readAt").asText()).isEqualTo(data.path("readAt").asText());
      assertThat(repeatedData.path("changed").asBoolean()).isFalse();
      assertThat(repeatedData.path("unreadCount").asLong()).isZero();

      Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
      assertThat(reloaded.getIsRead()).isTrue();
      assertThat(reloaded.getReadAt()).isNotNull();
      assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("기존 읽음 시각이 없으면 null과 changed false를 반환한다")
    void markAsRead_whenLegacyReadAtIsNull_returnsNullAndChangedFalse() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      Notification notification = seedNotification(user.getId(), "legacy");
      notification.markAsRead();
      org.springframework.test.util.ReflectionTestUtils.setField(notification, "readAt", null);
      notificationRepository.save(notification);

      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri(BASE + "/{notificationId}/read", notification.getId())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(data.path("readAt").isNull()).isTrue();
      assertThat(data.path("changed").asBoolean()).isFalse();
      assertThat(data.path("unreadCount").asLong()).isZero();
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
    @DisplayName("전체 읽음을 반복해도 최초 시각과 전달 상태를 보존한다")
    void markAllAsRead_whenRepeated_preservesFirstReadAtDeliveryStatusAndOtherUser()
        throws Exception {
      User user = userTestFactory.persistUser();
      User other = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      Notification pending = seedNotification(user.getId(), "pending", NotificationStatus.PENDING);
      Notification sent = seedNotification(user.getId(), "sent", NotificationStatus.SENT);
      Notification failed = seedNotification(user.getId(), "failed", NotificationStatus.FAILED);
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
      assertThat(data.path("updatedCount").asInt()).isEqualTo(3);

      entityManager.clear();
      List<Notification> firstReload =
          List.of(pending, sent, failed).stream()
              .map(
                  notification ->
                      notificationRepository.findById(notification.getId()).orElseThrow())
              .toList();
      assertThat(firstReload)
          .allSatisfy(notification -> assertThat(notification.getReadAt()).isNotNull());

      MvcTestResult repeated =
          mockMvcTester
              .patch()
              .uri(BASE + "/read-all")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      repeated.assertThat().hasStatusOk();
      assertThat(responseData(repeated).path("updatedCount").asInt()).isZero();

      entityManager.clear();
      List<Notification> secondReload =
          List.of(pending, sent, failed).stream()
              .map(
                  notification ->
                      notificationRepository.findById(notification.getId()).orElseThrow())
              .toList();
      assertThat(secondReload)
          .extracting(Notification::getReadAt)
          .containsExactlyElementsOf(firstReload.stream().map(Notification::getReadAt).toList());
      assertThat(secondReload)
          .extracting(Notification::getStatus)
          .containsExactly(
              NotificationStatus.PENDING, NotificationStatus.SENT, NotificationStatus.FAILED);

      assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
      Notification otherReloaded =
          notificationRepository.findById(otherNotification.getId()).orElseThrow();
      assertThat(otherReloaded.getIsRead()).isFalse();
      assertThat(otherReloaded.getReadAt()).isNull();
      assertThat(otherReloaded.getStatus()).isEqualTo(NotificationStatus.PENDING);
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
    return seedNotification(userId, title, NotificationStatus.PENDING);
  }

  private Notification seedNotification(
      Long userId, String title, NotificationStatus status) {
    return notificationRepository.save(
        Notification.builder()
            .userId(userId)
            .title(title)
            .content(title + "-content")
            .type(NotificationType.USER)
            .category(NotificationCategory.REVIEW)
            .status(status)
            .isRead(false)
            .build());
  }

  private Notification seedActionNotification(
      Long userId, String title, NotificationAction action) {
    return notificationRepository.save(
        Notification.builder()
            .userId(userId)
            .title(title)
            .content(title + "-content")
            .type(NotificationType.USER)
            .category(NotificationCategory.REVIEW)
            .action(action)
            .build());
  }

  private Notification seedRawActionNotification(
      Long userId, String title, String actionType, int actionVersion, JsonNode payload) {
    Notification notification = seedNotification(userId, title);
    org.springframework.test.util.ReflectionTestUtils.setField(
        notification, "actionType", actionType);
    org.springframework.test.util.ReflectionTestUtils.setField(notification, "actionTargetId", 10L);
    org.springframework.test.util.ReflectionTestUtils.setField(
        notification, "actionPayload", payload);
    org.springframework.test.util.ReflectionTestUtils.setField(
        notification, "actionVersion", (short) actionVersion);
    return notificationRepository.save(notification);
  }

  private JsonNode findItem(JsonNode items, Long notificationId) {
    return StreamSupport.stream(items.spliterator(), false)
        .filter(item -> item.path("id").asLong() == notificationId)
        .findFirst()
        .orElseThrow();
  }

  private JsonNode responseData(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private JsonNode responseMeta(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("meta");
  }

  private Set<String> fieldNames(JsonNode node) {
    return StreamSupport.stream(((Iterable<String>) () -> node.fieldNames()).spliterator(), false)
        .collect(java.util.stream.Collectors.toSet());
  }
}
