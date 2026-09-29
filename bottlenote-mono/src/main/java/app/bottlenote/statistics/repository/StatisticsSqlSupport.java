package app.bottlenote.statistics.repository;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.config.StatisticsProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/** 통계 JDBC 레포지토리가 공유하는 SQL 조각. 제외 규칙과 버킷 식은 지표마다 같아야 비교가 된다. */
final class StatisticsSqlSupport {

  /** ADR: 루트 관리자 제외는 user 도메인 테이블을 통계 SQL에서 직접 읽는다. */
  static final String NOT_ROOT_ADMIN_USER_ID = "NOT IN (SELECT user_id FROM root_admins)";

  private StatisticsSqlSupport() {}

  /** 텔레메트리 행 제외 조건. device_type 목록, IP prefix 목록, NULL IP, 루트 관리자 행을 뺀다. */
  static String telemetryExclusionSql(
      StatisticsProperties statisticsProperties, MapSqlParameterSource params) {
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
    sql.append(" AND (user_id IS NULL OR user_id ").append(NOT_ROOT_ADMIN_USER_ID).append(")");
    return sql.toString();
  }

  /** 일시 컬럼을 버킷 시작 날짜로 접는 식. 주는 월요일, 월은 1일 시작이다. */
  static String bucketExpression(TimeSeriesGranularity granularity, String column) {
    return switch (granularity) {
      case DAY -> "DATE(" + column + ")";
      case WEEK -> "DATE_SUB(DATE(" + column + "), INTERVAL WEEKDAY(" + column + ") DAY)";
      case MONTH -> "DATE_SUB(DATE(" + column + "), INTERVAL DAYOFMONTH(" + column + ") - 1 DAY)";
      case HOUR -> throw new IllegalArgumentException("HOUR는 회원·방문자 통계에서 지원하지 않습니다.");
    };
  }

  static LocalDateTime toBucketStart(Object value) {
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
