package app.bottlenote.statistics.fixture;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VisitorTelemetryTestFactory {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  private final JdbcTemplate jdbcTemplate;

  public void persistEvent(
      LocalDateTime occurredAt,
      String visitorId,
      Long userId,
      String ipAddress,
      String deviceType) {
    jdbcTemplate.update(
        """
        INSERT INTO visitor_telemetry_events (
            stream_event_id, occurred_at, visitor_id, user_id, ip_address,
            http_method, request_path, request_uri, normalized_request_path,
            status_code, duration_ms, device_type, operating_system, browser, is_webview
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        nextStreamEventId(),
        occurredAt,
        visitorId,
        userId,
        ipAddress,
        "GET",
        "/api/v1/alcohols",
        "/api/v1/alcohols",
        "/api/v1/alcohols",
        200,
        12L,
        deviceType,
        "iOS",
        "Safari",
        false);
  }

  private String nextStreamEventId() {
    return "t-" + COUNTER.incrementAndGet() + "-" + System.nanoTime();
  }
}
