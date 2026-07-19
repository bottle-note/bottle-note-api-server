package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] VisitorTelemetry 전송 모델")
class VisitorTelemetryTest {

  @Test
  @DisplayName("Redis Stream 필드를 영문 snake_case 문자열로 변환한다")
  void Redis_Stream_필드로_변환한다() {
    VisitorTelemetry telemetry =
        new VisitorTelemetry(
            LocalDateTime.of(2026, 7, 17, 1, 2, 3, 456_000_000),
            "visitor-hash",
            42L,
            "2001:db8::1",
            null,
            "GET",
            "/api/v1/alcohols?keyword=peated",
            "/api/v1/alcohols",
            "/api/v1/alcohols",
            200,
            15,
            "모바일",
            "Android",
            "Chrome",
            "143",
            true);
    VisitorTelemetry anonymousTelemetry =
        new VisitorTelemetry(
            LocalDateTime.of(2026, 7, 17, 1, 2, 3, 456_000_000),
            "visitor-hash",
            null,
            null,
            null,
            "GET",
            "/api/v1/alcohols",
            "/api/v1/alcohols",
            "/api/v1/alcohols",
            200,
            15,
            "모바일",
            "Android",
            "Chrome",
            "143",
            false);

    Map<String, String> fields = telemetry.toStreamFields();
    Map<String, String> anonymousFields = anonymousTelemetry.toStreamFields();

    assertThat(fields)
        .containsEntry("occurred_at", "2026-07-17T01:02:03.456000")
        .containsEntry("visitor_id", "visitor-hash")
        .containsEntry("user_id", "42")
        .containsEntry("ip_address", "2001:db8::1")
        .containsEntry("trace_id", "")
        .containsEntry("request_path", "/api/v1/alcohols?keyword=peated")
        .containsEntry("normalized_request_path", "/api/v1/alcohols")
        .containsEntry("is_webview", "true")
        .doesNotContainKeys("ip_hash", "user_agent_raw");
    assertThat(anonymousFields)
        .containsEntry("user_id", "")
        .containsEntry("ip_address", "");
  }
}
