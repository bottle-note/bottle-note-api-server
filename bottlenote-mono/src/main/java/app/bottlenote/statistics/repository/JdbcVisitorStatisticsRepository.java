package app.bottlenote.statistics.repository;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.config.StatisticsProperties;
import app.bottlenote.statistics.domain.ActiveVisitorBucket;
import app.bottlenote.statistics.domain.ReturningVisitorBucket;
import app.bottlenote.statistics.domain.VisitorStatisticsRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcVisitorStatisticsRepository implements VisitorStatisticsRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final StatisticsProperties statisticsProperties;

  @Override
  public List<ActiveVisitorBucket> countActiveVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    String sql =
        """
        SELECT %s AS bucket_at,
               COUNT(DISTINCT visitor_id) AS visitors,
               COUNT(DISTINCT user_id) AS members
        FROM visitor_telemetry_events
        WHERE occurred_at >= :from AND occurred_at < :toExclusive
        %s
        GROUP BY bucket_at
        ORDER BY bucket_at
        """
            .formatted(bucketExpression(granularity), exclusionSql(params));
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    return jdbcTemplate.query(sql, params, this::mapActive);
  }

  @Override
  public List<ReturningVisitorBucket> countReturningVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    String sql =
        """
        WITH visited AS (
          SELECT DISTINCT %s AS bucket_at, visitor_id
          FROM visitor_telemetry_events
          WHERE occurred_at >= :from AND occurred_at < :toExclusive
          %s
        )
        SELECT cur.bucket_at AS bucket_at,
               COUNT(DISTINCT cur.visitor_id) AS visitors,
               COUNT(DISTINCT prev.visitor_id) AS returning_visitors
        FROM visited cur
        LEFT JOIN visited prev
          ON prev.visitor_id = cur.visitor_id
         AND prev.bucket_at = %s
        GROUP BY cur.bucket_at
        ORDER BY cur.bucket_at
        """
            .formatted(
                bucketExpression(granularity),
                exclusionSql(params),
                previousBucketExpression(granularity));
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    return jdbcTemplate.query(sql, params, this::mapReturning);
  }

  private String exclusionSql(MapSqlParameterSource params) {
    StringBuilder sql = new StringBuilder();
    StatisticsProperties.Exclusion exclusion = statisticsProperties.getExclusion();
    List<String> deviceTypes = exclusion.getDeviceTypes();
    if (deviceTypes != null && !deviceTypes.isEmpty()) {
      sql.append(" AND device_type NOT IN (:deviceTypes)");
      params.addValue("deviceTypes", deviceTypes);
    }
    // prefix가 비어도 NULL IP는 제외한다. NOT LIKE만으로는 목록이 비면 NULL이 남는다.
    sql.append(" AND ip_address IS NOT NULL");
    List<String> ipPrefixes = exclusion.getIpPrefixes();
    if (ipPrefixes != null) {
      for (int index = 0; index < ipPrefixes.size(); index++) {
        String name = "ipPrefix" + index;
        sql.append(" AND ip_address NOT LIKE :").append(name);
        params.addValue(name, ipPrefixes.get(index) + "%");
      }
    }
    // ADR: 루트 관리자 제외는 user 도메인 테이블을 통계 SQL에서 직접 읽는다.
    sql.append(" AND (user_id IS NULL OR user_id NOT IN (SELECT user_id FROM root_admins))");
    return sql.toString();
  }

  private String bucketExpression(TimeSeriesGranularity granularity) {
    return switch (granularity) {
      case DAY -> "DATE(occurred_at)";
      case WEEK -> "DATE_SUB(DATE(occurred_at), INTERVAL WEEKDAY(occurred_at) DAY)";
      case MONTH -> "DATE_SUB(DATE(occurred_at), INTERVAL DAYOFMONTH(occurred_at) - 1 DAY)";
      case HOUR -> throw new IllegalArgumentException("HOUR는 방문자 통계에서 지원하지 않습니다.");
    };
  }

  private String previousBucketExpression(TimeSeriesGranularity granularity) {
    return switch (granularity) {
      case DAY -> "DATE_SUB(cur.bucket_at, INTERVAL 1 DAY)";
      case WEEK -> "DATE_SUB(cur.bucket_at, INTERVAL 7 DAY)";
      case MONTH -> "DATE_SUB(cur.bucket_at, INTERVAL 1 MONTH)";
      case HOUR -> throw new IllegalArgumentException("HOUR는 방문자 통계에서 지원하지 않습니다.");
    };
  }

  private ActiveVisitorBucket mapActive(ResultSet rs, int rowNum) throws SQLException {
    return new ActiveVisitorBucket(
        toBucketStart(rs.getObject("bucket_at")), rs.getLong("visitors"), rs.getLong("members"));
  }

  private ReturningVisitorBucket mapReturning(ResultSet rs, int rowNum) throws SQLException {
    return new ReturningVisitorBucket(
        toBucketStart(rs.getObject("bucket_at")),
        rs.getLong("visitors"),
        rs.getLong("returning_visitors"));
  }

  private LocalDateTime toBucketStart(Object value) {
    if (value instanceof LocalDate date) {
      return date.atStartOfDay();
    }
    if (value instanceof java.sql.Date date) {
      return date.toLocalDate().atStartOfDay();
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime().toLocalDate().atStartOfDay();
    }
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.toLocalDate().atStartOfDay();
    }
    throw new IllegalStateException("지원하지 않는 bucket_at 타입: " + value);
  }
}
