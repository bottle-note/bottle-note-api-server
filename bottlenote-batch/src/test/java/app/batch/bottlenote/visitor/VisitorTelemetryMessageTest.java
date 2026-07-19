package app.batch.bottlenote.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("batch")
@DisplayName("[batch] VisitorTelemetry 메시지 계약")
class VisitorTelemetryMessageTest {

  @Test
  @DisplayName("새 필드를 변환하고 기존 Stream의 선택 필드는 null로 처리한다")
  void 정상_필드를_변환한다() {
    Map<String, String> fields = validFields();
    fields.put("trace_id", "");
    fields.remove("browser_major_version");
    fields.put("future_field", "ignored");

    VisitorTelemetryMessage message = VisitorTelemetryMessage.from("1-0", fields);
    Map<String, String> legacyFields = validFields();
    legacyFields.remove("user_id");
    legacyFields.remove("ip_address");
    VisitorTelemetryMessage legacyMessage = VisitorTelemetryMessage.from("2-0", legacyFields);

    assertThat(message.streamEventId()).isEqualTo("1-0");
    assertThat(message.userId()).isEqualTo(42L);
    assertThat(message.ipAddress()).isEqualTo("2001:db8::1");
    assertThat(message.traceId()).isNull();
    assertThat(message.browserMajorVersion()).isNull();
    assertThat(message.webview()).isFalse();
    assertThat(legacyMessage.userId()).isNull();
    assertThat(legacyMessage.ipAddress()).isNull();
  }

  @Test
  @DisplayName("필수 필드가 없으면 영구 오류로 판정한다")
  void 필수_필드_누락을_거부한다() {
    Map<String, String> fields = validFields();
    fields.remove("visitor_id");

    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", fields))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("visitor_id");
  }

  @Test
  @DisplayName("필드 길이와 숫자와 시각 형식을 검증한다")
  void 필드_형식을_검증한다() {
    Map<String, String> tooLong = validFields();
    tooLong.put("browser", "b".repeat(31));
    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", tooLong))
        .hasMessageContaining("browser");

    Map<String, String> invalidNumber = validFields();
    invalidNumber.put("duration_ms", "not-number");
    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", invalidNumber))
        .hasMessageContaining("duration_ms");

    Map<String, String> invalidTime = validFields();
    invalidTime.put("occurred_at", "2026/07/17");
    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", invalidTime))
        .hasMessageContaining("occurred_at");

    Map<String, String> invalidUserId = validFields();
    invalidUserId.put("user_id", "not-number");
    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", invalidUserId))
        .hasMessageContaining("user_id");

    Map<String, String> tooLongIpAddress = validFields();
    tooLongIpAddress.put("ip_address", "a".repeat(46));
    assertThatThrownBy(() -> VisitorTelemetryMessage.from("1-0", tooLongIpAddress))
        .hasMessageContaining("ip_address");
  }

  static Map<String, String> validFields() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("occurred_at", "2026-07-17T01:20:13.717000");
    fields.put("visitor_id", "a".repeat(64));
    fields.put("user_id", "42");
    fields.put("ip_address", "2001:db8::1");
    fields.put("trace_id", "trace-id");
    fields.put("http_method", "GET");
    fields.put("request_path", "/api/v1/alcohols/search?keyword=test");
    fields.put("request_uri", "/api/v1/alcohols/search");
    fields.put("normalized_request_path", "/api/v1/alcohols/search");
    fields.put("status_code", "200");
    fields.put("duration_ms", "42");
    fields.put("device_type", "데스크톱");
    fields.put("operating_system", "macOS");
    fields.put("browser", "Chrome");
    fields.put("browser_major_version", "143");
    fields.put("is_webview", "false");
    return fields;
  }
}
