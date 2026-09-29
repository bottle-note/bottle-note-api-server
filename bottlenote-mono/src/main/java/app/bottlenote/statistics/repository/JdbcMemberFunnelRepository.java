package app.bottlenote.statistics.repository;

import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.statistics.config.StatisticsProperties;
import app.bottlenote.statistics.domain.MemberFunnelCounts;
import app.bottlenote.statistics.domain.MemberFunnelRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcMemberFunnelRepository implements MemberFunnelRepository {

  /** 약관 적격 판정은 AgreementEvaluator와 같다. 필수 유형마다 최신 이력(id 최대)이 AGREE여야 한다. */
  private static final List<String> REQUIRED_AGREEMENT_TYPES =
      Arrays.stream(AgreementType.values())
          .filter(AgreementType::isRequired)
          .map(Enum::name)
          .toList();

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final StatisticsProperties statisticsProperties;

  @Override
  public MemberFunnelCounts countFunnel(LocalDateTime from, LocalDateTime toExclusive) {
    long visitors = countVisitors(from, toExclusive);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    params.addValue("requiredTypes", REQUIRED_AGREEMENT_TYPES);
    params.addValue("requiredCount", REQUIRED_AGREEMENT_TYPES.size());
    String sql =
        """
        SELECT COUNT(*) AS signed_up,
               COALESCE(SUM(s.agreed), 0) AS agreed,
               COALESCE(SUM(s.activated), 0) AS activated
        FROM (
          SELECT u.id,
                 CASE WHEN (
                   SELECT COUNT(*)
                   FROM user_agreements ua
                   WHERE ua.user_id = u.id
                     AND ua.action = 'AGREE'
                     AND ua.agreement_type IN (:requiredTypes)
                     AND ua.id = (
                       SELECT MAX(latest.id)
                       FROM user_agreements latest
                       WHERE latest.user_id = u.id
                         AND latest.agreement_type = ua.agreement_type
                     )
                 ) = :requiredCount THEN 1 ELSE 0 END AS agreed,
                 CASE WHEN EXISTS (SELECT 1 FROM reviews r WHERE r.user_id = u.id)
                        OR EXISTS (SELECT 1 FROM ratings rt WHERE rt.user_id = u.id AND rt.rating > 0)
                        OR EXISTS (SELECT 1 FROM picks p WHERE p.user_id = u.id)
                      THEN 1 ELSE 0 END AS activated
          FROM users u
          WHERE u.create_at >= :from AND u.create_at < :toExclusive
            AND u.id %s
        ) s
        """
            .formatted(StatisticsSqlSupport.NOT_ROOT_ADMIN_USER_ID);
    return jdbcTemplate.queryForObject(
        sql,
        params,
        (rs, rowNum) ->
            new MemberFunnelCounts(
                visitors, rs.getLong("signed_up"), rs.getLong("agreed"), rs.getLong("activated")));
  }

  private long countVisitors(LocalDateTime from, LocalDateTime toExclusive) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    String sql =
        """
        SELECT COUNT(DISTINCT visitor_id)
        FROM visitor_telemetry_events
        WHERE occurred_at >= :from AND occurred_at < :toExclusive
        %s
        """
            .formatted(StatisticsSqlSupport.telemetryExclusionSql(statisticsProperties, params));
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    Long visitors = jdbcTemplate.queryForObject(sql, params, Long.class);
    return visitors == null ? 0L : visitors;
  }
}
