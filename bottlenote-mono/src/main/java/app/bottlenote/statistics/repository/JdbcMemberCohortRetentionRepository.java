package app.bottlenote.statistics.repository;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.domain.MemberCohort;
import app.bottlenote.statistics.domain.MemberCohortActivity;
import app.bottlenote.statistics.domain.MemberCohortRetentionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcMemberCohortRetentionRepository implements MemberCohortRetentionRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<MemberCohort> countCohorts(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    String sql =
        """
        SELECT %s AS cohort_at, COUNT(*) AS members
        FROM users u
        WHERE u.create_at >= :from AND u.create_at < :toExclusive
          AND u.id %s
        GROUP BY cohort_at
        ORDER BY cohort_at
        """
            .formatted(
                StatisticsSqlSupport.bucketExpression(granularity, "u.create_at"),
                StatisticsSqlSupport.NOT_ROOT_ADMIN_USER_ID);
    return jdbcTemplate.query(sql, params, this::mapCohort);
  }

  @Override
  public List<MemberCohortActivity> countActiveMembersByOffset(
      LocalDateTime from,
      LocalDateTime toExclusive,
      TimeSeriesGranularity granularity,
      int maxOffsetExclusive) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    params.addValue("maxOffsetExclusive", maxOffsetExclusive);
    // 활동일이 코호트 시작보다 앞선 행은 없어야 하지만, 롤업 재실행 순서에 상관없이 음수 offset을 막는다.
    String sql =
        """
        SELECT c.cohort_at AS cohort_at,
               %s AS offset_no,
               COUNT(DISTINCT a.user_id) AS active_members
        FROM (
          SELECT u.id AS user_id, %s AS cohort_at
          FROM users u
          WHERE u.create_at >= :from AND u.create_at < :toExclusive
            AND u.id %s
        ) c
        JOIN user_daily_activities a
          ON a.user_id = c.user_id
         AND a.activity_date >= c.cohort_at
        GROUP BY c.cohort_at, offset_no
        HAVING offset_no < :maxOffsetExclusive
        ORDER BY c.cohort_at, offset_no
        """
            .formatted(
                offsetExpression(granularity),
                StatisticsSqlSupport.bucketExpression(granularity, "u.create_at"),
                StatisticsSqlSupport.NOT_ROOT_ADMIN_USER_ID);
    return jdbcTemplate.query(sql, params, this::mapActivity);
  }

  @Override
  public Optional<LocalDate> findEarliestActivityDate() {
    LocalDate earliest =
        jdbcTemplate.queryForObject(
            "SELECT MIN(activity_date) FROM user_daily_activities",
            new MapSqlParameterSource(),
            (rs, rowNum) -> {
              java.sql.Date date = rs.getDate(1);
              return date == null ? null : date.toLocalDate();
            });
    return Optional.ofNullable(earliest);
  }

  /** 코호트 시작(버킷 시작일)에서 활동일까지의 버킷 수. 코호트가 버킷 시작이라 월 차이는 달력 기준이 된다. */
  private String offsetExpression(TimeSeriesGranularity granularity) {
    return switch (granularity) {
      case DAY -> "DATEDIFF(a.activity_date, c.cohort_at)";
      case WEEK -> "FLOOR(DATEDIFF(a.activity_date, c.cohort_at) / 7)";
      case MONTH -> "TIMESTAMPDIFF(MONTH, c.cohort_at, a.activity_date)";
      case HOUR -> throw new IllegalArgumentException("HOUR는 회원 코호트 통계에서 지원하지 않습니다.");
    };
  }

  private MemberCohort mapCohort(ResultSet rs, int rowNum) throws SQLException {
    return new MemberCohort(
        StatisticsSqlSupport.toBucketStart(rs.getObject("cohort_at")), rs.getLong("members"));
  }

  private MemberCohortActivity mapActivity(ResultSet rs, int rowNum) throws SQLException {
    return new MemberCohortActivity(
        StatisticsSqlSupport.toBucketStart(rs.getObject("cohort_at")),
        rs.getInt("offset_no"),
        rs.getLong("active_members"));
  }
}
