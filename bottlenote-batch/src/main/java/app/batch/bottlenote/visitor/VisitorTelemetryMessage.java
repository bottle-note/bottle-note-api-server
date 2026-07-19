package app.batch.bottlenote.visitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

record VisitorTelemetryMessage(
    String streamEventId,
    LocalDateTime occurredAt,
    String visitorId,
    Long userId,
    String ipAddress,
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

  static VisitorTelemetryMessage from(String streamEventId, Map<String, String> fields) {
    String userId = optional(fields, "user_id", 20);
    try {
      return new VisitorTelemetryMessage(
          required(streamEventId, "stream_event_id", 41),
          LocalDateTime.parse(required(fields, "occurred_at", 26)),
          required(fields, "visitor_id", 64),
          userId == null ? null : Long.valueOf(userId),
          optional(fields, "ip_address", 45),
          optional(fields, "trace_id", 64),
          required(fields, "http_method", 10),
          required(fields, "request_path", 2048),
          required(fields, "request_uri", 1024),
          required(fields, "normalized_request_path", 512),
          positiveInt(fields, "status_code", 100, 599),
          positiveLong(fields, "duration_ms"),
          required(fields, "device_type", 20),
          required(fields, "operating_system", 30),
          required(fields, "browser", 30),
          optional(fields, "browser_major_version", 20),
          strictBoolean(fields, "is_webview"));
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("occurred_at 형식이 올바르지 않습니다", exception);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("user_id 필드가 숫자가 아닙니다", exception);
    }
  }

  private static String required(Map<String, String> fields, String key, int maxLength) {
    return required(fields.get(key), key, maxLength);
  }

  private static String required(String value, String key, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(key + " 필드가 필요합니다");
    }
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(key + " 필드 길이가 " + maxLength + "자를 초과합니다");
    }
    return value;
  }

  private static String optional(Map<String, String> fields, String key, int maxLength) {
    String value = fields.get(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(key + " 필드 길이가 " + maxLength + "자를 초과합니다");
    }
    return value;
  }

  private static int positiveInt(
      Map<String, String> fields, String key, int minimum, int maximum) {
    String value = required(fields, key, 10);
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < minimum || parsed > maximum) {
        throw new IllegalArgumentException(key + " 필드 범위가 올바르지 않습니다");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(key + " 필드가 숫자가 아닙니다", exception);
    }
  }

  private static long positiveLong(Map<String, String> fields, String key) {
    String value = required(fields, key, 20);
    try {
      long parsed = Long.parseLong(value);
      if (parsed < 0) {
        throw new IllegalArgumentException(key + " 필드는 음수일 수 없습니다");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(key + " 필드가 숫자가 아닙니다", exception);
    }
  }

  private static boolean strictBoolean(Map<String, String> fields, String key) {
    String value = required(fields, key, 5);
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(key + " 필드가 boolean이 아닙니다");
  }
}
