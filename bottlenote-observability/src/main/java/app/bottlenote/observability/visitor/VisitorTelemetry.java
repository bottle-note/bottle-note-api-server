package app.bottlenote.observability.visitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** 쿠키 기반 방문자의 성공 API 요청을 나타내는 Redis Stream 전송 모델이다. */
public record VisitorTelemetry(
    LocalDateTime occurredAt,
    String visitorId,
    String traceId,
    String httpMethod,
    String requestPath,
    String requestUri,
    String normalizedRequestPath,
    int statusCode,
    long durationMs,
    String deviceType,
    String operatingSystem,
    String browser,
    String browserMajorVersion,
    boolean webview) {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

  /** Redis Stream에 기록할 영문 snake_case 필드를 반환한다. */
  public Map<String, String> toStreamFields() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("occurred_at", occurredAt.format(DATE_TIME_FORMATTER));
    fields.put("visitor_id", visitorId);
    fields.put("trace_id", traceId == null ? "" : traceId);
    fields.put("http_method", httpMethod);
    fields.put("request_path", requestPath);
    fields.put("request_uri", requestUri);
    fields.put("normalized_request_path", normalizedRequestPath);
    fields.put("status_code", Integer.toString(statusCode));
    fields.put("duration_ms", Long.toString(durationMs));
    fields.put("device_type", deviceType);
    fields.put("operating_system", operatingSystem);
    fields.put("browser", browser);
    fields.put("browser_major_version", browserMajorVersion);
    fields.put("is_webview", Boolean.toString(webview));
    return fields;
  }
}
