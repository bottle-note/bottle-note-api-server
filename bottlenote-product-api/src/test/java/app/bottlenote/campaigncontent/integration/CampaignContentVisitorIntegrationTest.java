package app.bottlenote.campaigncontent.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.campaigncontent.fixture.CampaignContentTestFactory;
import app.bottlenote.observability.visitor.VisitorTelemetryFilter;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** 테스트 프로파일은 방문자 필터가 꺼져 있어, 쿠키와 저장된 방문자 해시의 연결은 필터를 켠 컨텍스트에서 검증한다. */
@Tag("integration")
@DisplayName("[integration] 캠페인 콘텐츠 이벤트의 방문자 식별")
@TestPropertySource(properties = "bottlenote.observability.visitor-telemetry.enabled=true")
class CampaignContentVisitorIntegrationTest extends IntegrationTestSupport {

  private static final String USER_AGENT =
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
          + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

  @Autowired private CampaignContentTestFactory campaignContentTestFactory;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("쿠키 없는 첫 VIEW를 보낼 때 발급한 방문자 쿠키의 해시로 기록하고 다음 이벤트도 같은 방문자로 이어진다")
  void 첫_VIEW와_다음_이벤트가_같은_방문자로_이어진다() throws Exception {
    campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI");

    MvcTestResult first = post("VIEW", null);

    assertThat(first).hasStatusOk();
    String issuedVisitorId = cookieValue(first.getResponse().getHeader(HttpHeaders.SET_COOKIE));

    MvcTestResult second =
        post("START", new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, issuedVisitorId));

    assertThat(second).hasStatusOk();
    assertThat(second.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            "SELECT event_type, visitor_id, ip_address, device_type"
                + " FROM campaign_content_events ORDER BY id");
    assertThat(rows).hasSize(2);
    assertThat(rows)
        .allSatisfy(
            row -> {
              assertThat(row.get("visitor_id")).isEqualTo(sha256(issuedVisitorId));
              assertThat(row.get("ip_address")).isNotNull();
              assertThat(row.get("device_type")).isEqualTo("모바일");
            });
  }

  private MvcTestResult post(String type, Cookie cookie) {
    var request =
        mockMvcTester
            .post()
            .uri("/api/v1/campaign-contents/whiskey-mbti/events")
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .contentType(APPLICATION_JSON)
            .content("{\"type\":\"" + type + "\"}");
    if (cookie != null) {
      request = request.cookie(cookie);
    }
    return request.exchange();
  }

  private static String cookieValue(String setCookie) {
    assertThat(setCookie).startsWith(VisitorTelemetryFilter.VISITOR_COOKIE_NAME + "=");
    return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
  }

  private static String sha256(String value) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
