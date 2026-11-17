package app.bottlenote.campaigncontent.repository;

import app.bottlenote.campaigncontent.domain.CampaignContentEventCounts;
import app.bottlenote.campaigncontent.domain.CampaignContentExclusion;
import app.bottlenote.campaigncontent.domain.CampaignContentMetricsRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcCampaignContentMetricsRepository implements CampaignContentMetricsRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public CampaignContentEventCounts countEvents(
      Long campaignContentId,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    // 완주·전환 교집합은 같은 visitor_id 안에서 판정해야 기간·기기 경계에서 100%를 넘지 않는다.
    String sql =
        """
        WITH scoped AS (
          SELECT visitor_id, user_id, event_type
          FROM campaign_content_events
          WHERE campaign_content_id = :campaignContentId
            AND occurred_at >= :from AND occurred_at < :toExclusive
          %s
        ),
        per_visitor AS (
          SELECT visitor_id,
                 MAX(event_type = 'START') AS started,
                 MAX(event_type = 'FINISH') AS finished,
                 MAX(event_type = 'RESULT') AS resulted
          FROM scoped
          WHERE visitor_id IS NOT NULL
          GROUP BY visitor_id
        )
        SELECT
          (SELECT COUNT(DISTINCT visitor_id) FROM scoped WHERE event_type = 'VIEW') AS view_visitors,
          (SELECT COUNT(*) FROM per_visitor WHERE started = 1) AS start_visitors,
          (SELECT COUNT(*) FROM per_visitor WHERE finished = 1) AS finish_visitors,
          (SELECT COUNT(DISTINCT user_id) FROM scoped WHERE event_type = 'RESULT') AS result_members,
          (SELECT COUNT(*) FROM per_visitor WHERE started = 1 AND finished = 1)
            AS started_and_finished_visitors,
          (SELECT COUNT(*) FROM per_visitor WHERE finished = 1 AND resulted = 1)
            AS finished_and_result_visitors
        """
            .formatted(exclusionSql(exclusion, params));
    params.addValue("campaignContentId", campaignContentId);
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    return jdbcTemplate.queryForObject(
        sql,
        params,
        (rs, rowNum) ->
            new CampaignContentEventCounts(
                rs.getLong("view_visitors"),
                rs.getLong("start_visitors"),
                rs.getLong("finish_visitors"),
                rs.getLong("result_members"),
                rs.getLong("started_and_finished_visitors"),
                rs.getLong("finished_and_result_visitors")));
  }

  @Override
  public Map<Long, Long> countResultMembers(
      Collection<Long> campaignContentIds,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion) {
    if (campaignContentIds == null || campaignContentIds.isEmpty()) {
      return Map.of();
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    String sql =
        """
        SELECT campaign_content_id, COUNT(DISTINCT user_id) AS members
        FROM campaign_content_events
        WHERE campaign_content_id IN (:campaignContentIds)
          AND event_type = 'RESULT'
          AND occurred_at >= :from AND occurred_at < :toExclusive
          %s
        GROUP BY campaign_content_id
        """
            .formatted(exclusionSql(exclusion, params));
    params.addValue("campaignContentIds", campaignContentIds);
    params.addValue("from", from);
    params.addValue("toExclusive", toExclusive);
    Map<Long, Long> result = new HashMap<>();
    jdbcTemplate.query(
        sql,
        params,
        rs -> {
          result.put(rs.getLong("campaign_content_id"), rs.getLong("members"));
        });
    return result;
  }

  // 방문자 통계(JdbcVisitorStatisticsRepository)와 같은 제외 조건을 이벤트 테이블에 적용한다.
  private String exclusionSql(CampaignContentExclusion exclusion, MapSqlParameterSource params) {
    StringBuilder sql = new StringBuilder();
    List<String> deviceTypes = exclusion.deviceTypes();
    if (!deviceTypes.isEmpty()) {
      sql.append(" AND device_type NOT IN (:deviceTypes)");
      params.addValue("deviceTypes", deviceTypes);
    }
    sql.append(" AND ip_address IS NOT NULL");
    List<String> ipPrefixes = exclusion.ipPrefixes();
    for (int index = 0; index < ipPrefixes.size(); index++) {
      String name = "ipPrefix" + index;
      sql.append(" AND ip_address NOT LIKE :").append(name);
      params.addValue(name, ipPrefixes.get(index) + "%");
    }
    sql.append(" AND (user_id IS NULL OR user_id NOT IN (SELECT user_id FROM root_admins))");
    return sql.toString();
  }
}
