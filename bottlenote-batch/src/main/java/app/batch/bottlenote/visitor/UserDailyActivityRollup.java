package app.batch.bottlenote.visitor;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 텔레메트리의 회원 요청을 회원·일 단위 한 행으로 접어 user_daily_activities에 쓴다.
 *
 * <p>텔레메트리는 90일이 지나면 지워지므로 코호트 리텐션처럼 긴 기간을 보는 지표는 이 롤업을 읽는다. 같은 회원·같은 날은 덮어쓰기라 재실행해도 결과가 같다.
 */
@RequiredArgsConstructor
public class UserDailyActivityRollup {

  /** 텔레메트리 보존 기간. 첫 실행은 남아 있는 원본 전체를 채운다. */
  static final int TELEMETRY_RETENTION_DAYS = 90;

  /** 마지막 롤업일을 다시 접는다. 스트림 소비가 늦어 자정 이후 도착한 전날 행을 흡수한다. */
  static final int RE_ROLLUP_DAYS = 1;

  private static final String UPSERT_SQL =
      """
      INSERT INTO user_daily_activities
          (user_id, activity_date, request_count, first_seen_at, last_seen_at, recorded_at)
      SELECT user_id,
             CAST(occurred_at AS DATE),
             COUNT(*),
             MIN(occurred_at),
             MAX(occurred_at),
             ?
      FROM visitor_telemetry_events
      WHERE user_id IS NOT NULL
        AND occurred_at >= ? AND occurred_at < ?
      GROUP BY user_id, CAST(occurred_at AS DATE)
      ON DUPLICATE KEY UPDATE
          request_count = VALUES(request_count),
          first_seen_at = VALUES(first_seen_at),
          last_seen_at  = VALUES(last_seen_at),
          recorded_at   = VALUES(recorded_at)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  /**
   * today 직전까지의 원본을 접는다. 시작일은 마지막 롤업일 하루 전이며, 롤업이 비어 있으면 보존 기간 시작이다.
   *
   * @return 접은 구간, 원본이 없으면 시작일과 끝일이 같은 빈 구간
   */
  public RollupRange rollup(LocalDate today, LocalDateTime recordedAt) {
    LocalDate retentionStart = today.minusDays(TELEMETRY_RETENTION_DAYS);
    LocalDate lastRolled = findLastActivityDate();
    LocalDate start =
        lastRolled == null ? retentionStart : lastRolled.minusDays(RE_ROLLUP_DAYS);
    if (start.isBefore(retentionStart)) {
      start = retentionStart;
    }
    if (!start.isBefore(today)) {
      return new RollupRange(today, today, 0);
    }
    LocalDate from = start;
    Integer affected =
        transactionTemplate.execute(
            status ->
                jdbcTemplate.update(
                    UPSERT_SQL, recordedAt, from.atStartOfDay(), today.atStartOfDay()));
    return new RollupRange(from, today, affected == null ? 0 : affected);
  }

  private LocalDate findLastActivityDate() {
    Date date =
        jdbcTemplate.queryForObject(
            "SELECT MAX(activity_date) FROM user_daily_activities", Date.class);
    return date == null ? null : date.toLocalDate();
  }

  /**
   * @param from 접은 첫날
   * @param toExclusive 접지 않은 첫날
   * @param affectedRows JDBC가 보고한 영향 행 수. MySQL은 갱신 행을 2로 세므로 행 수와 다를 수 있다
   */
  public record RollupRange(LocalDate from, LocalDate toExclusive, int affectedRows) {}
}
