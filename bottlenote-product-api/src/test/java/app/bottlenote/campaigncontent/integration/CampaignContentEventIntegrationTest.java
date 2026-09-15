package app.bottlenote.campaigncontent.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode;
import app.bottlenote.campaigncontent.fixture.CampaignContentTestFactory;
import app.bottlenote.campaigncontent.repository.JpaCampaignContentEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] CampaignContentEventController")
class CampaignContentEventIntegrationTest extends IntegrationTestSupport {

  @Autowired private CampaignContentTestFactory campaignContentTestFactory;
  @Autowired private JpaCampaignContentEventRepository campaignContentEventRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Nested
  @DisplayName("참여 이벤트 기록")
  class RegisterParticipation {

    @Test
    @DisplayName("비로그인 사용자가 VIEW를 보낼 때 회원 없이 이벤트를 기록한다")
    void 비로그인_VIEW를_기록할_수_있다() {
      campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI");

      MvcTestResult result = post("whiskey-mbti", "VIEW", null);

      assertThat(result).hasStatusOk();
      assertThat(result).bodyJson().extractingPath("$.data.code").isEqualTo("whiskey-mbti");
      assertThat(result).bodyJson().extractingPath("$.data.type").isEqualTo("VIEW");
      List<Map<String, Object>> rows = events();
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("event_type")).isEqualTo("VIEW");
      assertThat(rows.get(0).get("user_id")).isNull();
    }

    @Test
    @DisplayName("로그인 사용자가 RESULT를 보낼 때 회원 ID와 함께 기록한다")
    void 로그인_RESULT를_기록할_수_있다() {
      campaignContentTestFactory.persistCampaignContent("whiskey-tarot", "위스키 타로");
      String accessToken = getToken();
      Long userId = getTokenUserId();

      MvcTestResult result = post("whiskey-tarot", "RESULT", accessToken);

      assertThat(result).hasStatusOk();
      List<Map<String, Object>> rows = events();
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("event_type")).isEqualTo("RESULT");
      assertThat(((Number) rows.get(0).get("user_id")).longValue()).isEqualTo(userId);
    }

    @Test
    @DisplayName("비로그인 사용자가 RESULT를 보낼 때 400으로 거절하고 기록하지 않는다")
    void 비로그인_RESULT는_거절한다() {
      campaignContentTestFactory.persistCampaignContent("whiskey-tarot", "위스키 타로");

      MvcTestResult result = post("whiskey-tarot", "RESULT", null);

      assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
      assertThat(events()).isEmpty();
    }

    @Test
    @DisplayName("등록되지 않은 코드로 보낼 때 404로 거절한다")
    void 미등록_코드는_404다() {
      MvcTestResult result = post("unknown-content", "VIEW", null);

      assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 코드로 보낼 때 404로 거절하고 기록하지 않는다")
    void 비활성_코드는_404다() {
      campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI", false);

      MvcTestResult result = post("whiskey-mbti", "VIEW", null);

      assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
      assertThat(events()).isEmpty();
    }

    @Test
    @DisplayName("이벤트 유형이 없을 때 400으로 거절한다")
    void 이벤트_유형이_없으면_400이다() {
      campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI");

      MvcTestResult result =
          mockMvcTester
              .post()
              .uri("/api/v1/campaign-contents/whiskey-mbti/events")
              .contentType(APPLICATION_JSON)
              .content("{}")
              .exchange();

      assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
      assertThat(events()).isEmpty();
    }

    @Test
    @DisplayName("콘텐츠 삭제가 먼저 커밋된 뒤 이벤트를 저장하면 NOT_FOUND로 변환한다")
    void 삭제된_콘텐츠의_이벤트_저장은_NOT_FOUND다() {
      CampaignContent content =
          campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI");
      jdbcTemplate.update("DELETE FROM campaign_contents WHERE id = ?", content.getId());

      CampaignContentEventLog event =
          CampaignContentEventLog.builder()
              .campaignContentId(content.getId())
              .eventType(CampaignContentEventType.VIEW)
              .visitorId("a".repeat(64))
              .ipAddress("203.0.113.10")
              .deviceType("모바일")
              .occurredAt(LocalDateTime.now())
              .build();

      assertThatThrownBy(() -> campaignContentEventRepository.register(event))
          .isInstanceOf(CampaignContentException.class)
          .extracting("exceptionCode")
          .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND);
    }
  }

  private MvcTestResult post(String code, String type, String accessToken) {
    var request =
        mockMvcTester
            .post()
            .uri("/api/v1/campaign-contents/{code}/events", code)
            .contentType(APPLICATION_JSON)
            .content("{\"type\":\"" + type + "\"}");
    if (accessToken != null) {
      request = request.header("Authorization", "Bearer " + accessToken);
    }
    return request.exchange();
  }

  private List<Map<String, Object>> events() {
    return jdbcTemplate.queryForList(
        "SELECT event_type, user_id, visitor_id FROM campaign_content_events ORDER BY id");
  }
}
