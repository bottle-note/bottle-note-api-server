package app.batch.bottlenote.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("batch")
@DisplayName("[batch] 회원 일별 활동 롤업")
class UserDailyActivityRollupTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
  private static final LocalDateTime RECORDED_AT = TODAY.atTime(3, 10);

  private JdbcTemplate jdbc;
  private UserDailyActivityRollup rollup;

  @BeforeEach
  void setUp() {
    jdbc = UserDailyActivitySqlSupport.freshDatabase();
    rollup =
        new UserDailyActivityRollup(
            jdbc, new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())));
  }

  @Test
  @DisplayName("회원 요청을 회원·일 단위 한 행으로 접고 비회원 요청은 무시한다")
  void 회원별_일별_한_행으로_접을_수_있다() {
    LocalDate yesterday = TODAY.minusDays(1);
    insertEvent(yesterday.atTime(9, 0), "A", 1L);
    insertEvent(yesterday.atTime(23, 59), "A", 1L);
    insertEvent(yesterday.atTime(12, 0), "B", 2L);
    insertEvent(yesterday.atTime(12, 0), "C", null);
    insertEvent(TODAY.atTime(1, 0), "A", 1L);

    UserDailyActivityRollup.RollupRange range = rollup.rollup(TODAY, RECORDED_AT);

    assertThat(range.from()).isEqualTo(TODAY.minusDays(90));
    assertThat(range.toExclusive()).isEqualTo(TODAY);
    List<Map<String, Object>> rows = rows();
    assertThat(rows).hasSize(2);
    Map<String, Object> first = rows.get(0);
    assertThat(first.get("user_id")).isEqualTo(1L);
    assertThat(((Number) first.get("request_count")).intValue()).isEqualTo(2);
    assertThat(((Timestamp) first.get("first_seen_at")).toLocalDateTime())
        .isEqualTo(yesterday.atTime(9, 0));
    assertThat(((Timestamp) first.get("last_seen_at")).toLocalDateTime())
        .isEqualTo(yesterday.atTime(23, 59));
    assertThat(((Timestamp) first.get("recorded_at")).toLocalDateTime()).isEqualTo(RECORDED_AT);
    assertThat(rows.get(1).get("user_id")).isEqualTo(2L);
  }

  @Test
  @DisplayName("재실행하면 마지막 롤업일부터 다시 접어 늦게 도착한 행을 흡수하고 중복 행을 만들지 않는다")
  void 재실행해도_중복_없이_덮어쓸_수_있다() {
    LocalDate twoDaysAgo = TODAY.minusDays(2);
    LocalDate yesterday = TODAY.minusDays(1);
    insertEvent(twoDaysAgo.atTime(10, 0), "A", 1L);
    insertEvent(yesterday.atTime(10, 0), "A", 1L);
    rollup.rollup(TODAY, RECORDED_AT);
    // 스트림 소비가 늦어 전날 행이 롤업 뒤에 도착했다
    insertEvent(yesterday.atTime(22, 0), "A", 1L);
    insertEvent(twoDaysAgo.atTime(22, 0), "A", 1L);

    UserDailyActivityRollup.RollupRange range = rollup.rollup(TODAY, RECORDED_AT.plusHours(1));

    assertThat(range.from()).isEqualTo(twoDaysAgo);
    List<Map<String, Object>> rows = rows();
    assertThat(rows).hasSize(2);
    // 전날은 다시 접혀 2건, 이틀 전은 재롤업 범위 밖이라 1건으로 남는다
    assertThat(((Number) rows.get(0).get("request_count")).intValue()).isEqualTo(2);
    assertThat(((Number) rows.get(1).get("request_count")).intValue()).isEqualTo(2);
  }

  @Test
  @DisplayName("오늘 행은 아직 닫히지 않았으므로 접지 않는다")
  void 오늘은_접지_않는다() {
    insertEvent(TODAY.atTime(8, 0), "A", 1L);

    rollup.rollup(TODAY, RECORDED_AT);

    assertThat(rows()).isEmpty();
  }

  @Test
  @DisplayName("보존 기간 밖의 원본은 첫 실행에서도 읽지 않는다")
  void 보존_기간_밖은_읽지_않는다() {
    insertEvent(TODAY.minusDays(91).atTime(8, 0), "A", 1L);
    insertEvent(TODAY.minusDays(90).atTime(8, 0), "A", 1L);

    rollup.rollup(TODAY, RECORDED_AT);

    assertThat(rows()).extracting(row -> row.get("activity_date").toString())
        .containsExactly(TODAY.minusDays(90).toString());
  }

  private void insertEvent(LocalDateTime occurredAt, String visitorId, Long userId) {
    jdbc.update(
        "INSERT INTO visitor_telemetry_events (occurred_at, visitor_id, user_id) VALUES (?, ?, ?)",
        occurredAt,
        visitorId,
        userId);
  }

  private List<Map<String, Object>> rows() {
    return jdbc.queryForList(
        "SELECT user_id, activity_date, request_count, first_seen_at, last_seen_at, recorded_at"
            + " FROM user_daily_activities ORDER BY user_id, activity_date");
  }
}
