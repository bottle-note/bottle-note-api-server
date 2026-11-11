package app.bottlenote.history.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import app.bottlenote.history.constant.RedirectUrlType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcHistoryAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params =
        Map.of(
            "source",
            sourceAlcoholId,
            "target",
            targetAlcoholId,
            "sourceUrl",
            RedirectUrlType.ALCOHOL.getUrl() + "/" + sourceAlcoholId,
            "targetUrl",
            RedirectUrlType.ALCOHOL.getUrl() + "/" + targetAlcoholId);
    int movedHistories =
        jdbcTemplate.update(
            """
            UPDATE user_histories
            SET alcohol_id = :target,
                redirect_url = IF(redirect_url = :sourceUrl, :targetUrl, redirect_url)
            WHERE alcohol_id = :source
            """,
            params);
    // 두 위스키를 모두 본 사용자는 더 최근 조회 시각을 기준 위스키에 남긴다
    jdbcTemplate.update(
        """
        UPDATE alcohols_view_histories t
        JOIN alcohols_view_histories s ON s.user_id = t.user_id AND s.alcohol_id = :source
        SET t.view_at = GREATEST(t.view_at, s.view_at)
        WHERE t.alcohol_id = :target
        """,
        params);
    int discardedViews =
        jdbcTemplate.update(
            """
            DELETE s FROM alcohols_view_histories s
            JOIN alcohols_view_histories t ON t.user_id = s.user_id AND t.alcohol_id = :target
            WHERE s.alcohol_id = :source
            """,
            params);
    int movedViews =
        jdbcTemplate.update(
            "UPDATE alcohols_view_histories SET alcohol_id = :target WHERE alcohol_id = :source",
            params);
    return List.of(
        new AlcoholMergeCount("userHistories", movedHistories, 0),
        new AlcoholMergeCount("viewHistories", movedViews, discardedViews));
  }
}
