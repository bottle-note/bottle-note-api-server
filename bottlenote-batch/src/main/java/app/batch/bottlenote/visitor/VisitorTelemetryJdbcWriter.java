package app.batch.bottlenote.visitor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
final class VisitorTelemetryJdbcWriter {

  private static final String INSERT_SQL =
      """
      INSERT INTO visitor_telemetry_events (
          stream_event_id, occurred_at, visitor_id, trace_id, http_method,
          request_path, request_uri, normalized_request_path, status_code, duration_ms,
          device_type, operating_system, browser, browser_major_version, is_webview
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE stream_event_id = VALUES(stream_event_id)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  void write(List<VisitorTelemetryMessage> messages) {
    if (messages.isEmpty()) {
      return;
    }

    transactionTemplate.executeWithoutResult(
        status ->
            jdbcTemplate.batchUpdate(
                INSERT_SQL,
                messages,
                messages.size(),
                (statement, message) -> {
                  statement.setString(1, message.streamEventId());
                  statement.setObject(2, message.occurredAt());
                  statement.setString(3, message.visitorId());
                  statement.setString(4, message.traceId());
                  statement.setString(5, message.httpMethod());
                  statement.setString(6, message.requestPath());
                  statement.setString(7, message.requestUri());
                  statement.setString(8, message.normalizedRequestPath());
                  statement.setInt(9, message.statusCode());
                  statement.setLong(10, message.durationMs());
                  statement.setString(11, message.deviceType());
                  statement.setString(12, message.operatingSystem());
                  statement.setString(13, message.browser());
                  statement.setString(14, message.browserMajorVersion());
                  statement.setBoolean(15, message.webview());
                }));
  }
}
