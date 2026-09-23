package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.constant.NotificationSettingGroup;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.notification.service.NotificationSettingService;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] 알림 수신 설정")
class NotificationSettingControllerIntegrationTest extends IntegrationTestSupport {

  private static final String SETTINGS = "/api/v1/notifications/settings";

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private NotificationSettingService settingService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("설정이 없을 때 모든 그룹과 발생 액션을 기본값으로 조회한다")
  void 기본_설정을_조회할_수_있다() throws Exception {
    TokenItem token = getToken(userTestFactory.persistUser());

    MvcTestResult result = get(token);

    result.assertThat().hasStatusOk();
    JsonNode groups = responseData(result).path("groups");
    assertThat(texts(groups, "group"))
        .containsExactlyElementsOf(
            Arrays.stream(NotificationSettingGroup.values()).map(Enum::name).toList());
    List<JsonNode> items = items(groups);
    assertThat(items).hasSize(NotificationEventAction.values().length);
    assertThat(items)
        .allSatisfy(
            item -> {
              NotificationEventAction action =
                  NotificationEventAction.valueOf(item.path("eventAction").asText());
              assertThat(item.path("displayName").asText()).isEqualTo(action.getDisplayName());
              assertThat(item.path("description").asText()).isEqualTo(action.getDescription());
              assertThat(item.path("defaultEnabled").asBoolean())
                  .isEqualTo(action.isDefaultEnabled());
              assertThat(item.path("enabled").asBoolean()).isEqualTo(action.isDefaultEnabled());
            });
    assertThat(groups.get(0).path("displayName").asText())
        .isEqualTo(NotificationSettingGroup.REVIEW_AND_FOLLOW.getDescription());
  }

  @Test
  @DisplayName("여러 발생 액션을 한 번에 변경하고 본인 설정만 반영한다")
  void 여러_설정을_변경할_수_있다() throws Exception {
    var user = userTestFactory.persistUser();
    var other = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    MvcTestResult result =
        patch(
            token,
            """
            {"settings":[
              {"eventAction":"REVIEW_LIKE_ADD","enabled":false},
              {"eventAction":"FOLLOW_CREATE","enabled":false}
            ]}
            """);

    result.assertThat().hasStatusOk();
    List<JsonNode> items = items(responseData(result).path("groups"));
    assertThat(items)
        .filteredOn(item -> !item.path("enabled").asBoolean())
        .extracting(item -> item.path("eventAction").asText())
        .containsExactlyInAnyOrder("REVIEW_LIKE_ADD", "FOLLOW_CREATE");
    assertThat(items(responseData(get(token)).path("groups")))
        .filteredOn(item -> !item.path("enabled").asBoolean())
        .hasSize(2);
    assertThat(settingService.getSettings(other.getId())).doesNotContainValue(false);
  }

  @Test
  @DisplayName("기본값으로 되돌리면 저장된 설정을 지우고 같은 요청을 반복해도 성공한다")
  void 기본값으로_되돌릴_수_있다() throws Exception {
    var user = userTestFactory.persistUser();
    TokenItem token = getToken(user);
    settingService.changeSetting(user.getId(), NotificationEventAction.REVIEW_LIKE_ADD, false);
    String body =
        """
        {"settings":[{"eventAction":"REVIEW_LIKE_ADD","enabled":true}]}
        """;

    patch(token, body).assertThat().hasStatusOk();
    patch(token, body).assertThat().hasStatusOk();

    assertThat(settingCount(user.getId())).isZero();
    assertThat(settingService.getSettings(user.getId())).doesNotContainValue(false);
  }

  @RepeatedTest(5)
  @DisplayName("상반된 일괄 변경이 동시에 들어와도 한 요청의 값만 모두 반영한다")
  void 동시_일괄_변경이_섞이지_않는다() throws Exception {
    Long userId = userTestFactory.persistUser().getId();
    NotificationEventAction a = NotificationEventAction.REVIEW_LIKE_ADD;
    NotificationEventAction b = NotificationEventAction.FOLLOW_CREATE;
    settingService.changeSetting(userId, a, false);
    List<Map<NotificationEventAction, Boolean>> requests =
        List.of(Map.of(a, false, b, false), Map.of(a, true, b, true));

    CountDownLatch ready = new CountDownLatch(requests.size());
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(requests.size())) {
      List<Future<?>> futures = new ArrayList<>();
      for (var changes : requests) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("동시 실행 대기 초과");
                  settingService.changeSettings(userId, changes);
                  return null;
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (var future : futures) future.get(20, TimeUnit.SECONDS);
    }

    Map<NotificationEventAction, Boolean> result = settingService.getSettings(userId);
    assertThat(result.get(a)).isEqualTo(result.get(b));
  }

  @Test
  @DisplayName("같은 발생 액션을 중복 지정하면 아무것도 변경하지 않고 400을 반환한다")
  void 중복_지정을_거부한다() throws Exception {
    var user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    MvcTestResult result =
        patch(
            token,
            """
            {"settings":[
              {"eventAction":"TASTING_OPEN","enabled":false},
              {"eventAction":"REVIEW_LIKE_ADD","enabled":false},
              {"eventAction":"REVIEW_LIKE_ADD","enabled":true}
            ]}
            """);

    result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorCode(result))
        .isEqualTo(NotificationExceptionCode.DUPLICATE_NOTIFICATION_SETTING.name());
    assertThat(settingCount(user.getId())).isZero();
  }

  @Test
  @DisplayName("빈 변경·누락 필드·정의되지 않은 발생 액션을 거부한다")
  void 잘못된_요청을_거부한다() throws Exception {
    var user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    assertThat(errorCode(patch(token, "{\"settings\":[]}")))
        .isEqualTo(ValidExceptionCode.NOTIFICATION_SETTINGS_REQUIRED.name());
    assertThat(errorCode(patch(token, "{\"settings\":[null]}")))
        .isEqualTo(ValidExceptionCode.NOTIFICATION_SETTING_REQUIRED.name());
    assertThat(errorCode(patch(token, "{\"settings\":[{\"enabled\":false}]}")))
        .isEqualTo(ValidExceptionCode.NOTIFICATION_EVENT_ACTION_REQUIRED.name());
    assertThat(errorCode(patch(token, "{\"settings\":[{\"eventAction\":\"FOLLOW_CREATE\"}]}")))
        .isEqualTo(ValidExceptionCode.NOTIFICATION_ENABLED_REQUIRED.name());
    patch(token, "{\"settings\":[{\"eventAction\":\"UNKNOWN\",\"enabled\":false}]}")
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(settingCount(user.getId())).isZero();
  }

  @Test
  @DisplayName("인증 정보가 없으면 401을 반환한다")
  void 미인증_요청을_거부한다() {
    mockMvcTester.get().uri(SETTINGS).exchange().assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    mockMvcTester
        .patch()
        .uri(SETTINGS)
        .contentType(APPLICATION_JSON)
        .content("{\"settings\":[{\"eventAction\":\"FOLLOW_CREATE\",\"enabled\":false}]}")
        .with(csrf())
        .exchange()
        .assertThat()
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  private MvcTestResult get(TokenItem token) {
    return mockMvcTester
        .get()
        .uri(SETTINGS)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
        .exchange();
  }

  private MvcTestResult patch(TokenItem token, String body) {
    return mockMvcTester
        .patch()
        .uri(SETTINGS)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
        .contentType(APPLICATION_JSON)
        .content(body)
        .with(csrf())
        .exchange();
  }

  private long settingCount(Long userId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_notification_settings WHERE user_id = ?", Long.class, userId);
  }

  private JsonNode responseData(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private String errorCode(MvcTestResult result) throws Exception {
    return mapper
        .readTree(result.getResponse().getContentAsString())
        .path("errors")
        .get(0)
        .path("code")
        .asText();
  }

  private List<String> texts(JsonNode array, String field) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(node -> node.path(field).asText())
        .toList();
  }

  private List<JsonNode> items(JsonNode groups) {
    return StreamSupport.stream(groups.spliterator(), false)
        .flatMap(group -> StreamSupport.stream(group.path("settings").spliterator(), false))
        .toList();
  }
}
