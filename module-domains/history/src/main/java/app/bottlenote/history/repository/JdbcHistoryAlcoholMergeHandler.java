package app.bottlenote.history.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import app.bottlenote.history.constant.RedirectUrlType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcHistoryAlcoholMergeHandler implements AlcoholMergeHandler {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long duplicateAlcoholId, Long baseAlcoholId) {
    Map<String, Object> params =
        Map.of(
            "duplicate",
            duplicateAlcoholId,
            "base",
            baseAlcoholId,
            "duplicateUrl",
            RedirectUrlType.ALCOHOL.getUrl() + "/" + duplicateAlcoholId,
            "baseUrl",
            RedirectUrlType.ALCOHOL.getUrl() + "/" + baseAlcoholId);
    int movedHistories =
        jdbcTemplate.update(
            """
            UPDATE user_histories
            SET alcohol_id = :base,
                redirect_url = IF(redirect_url = :duplicateUrl, :baseUrl, redirect_url)
            WHERE alcohol_id = :duplicate
            """,
            params);
    // 두 위스키를 모두 본 사용자는 더 최근 조회 시각을 기준 위스키에 남긴다
    jdbcTemplate.update(
        """
        UPDATE alcohols_view_histories b
        JOIN alcohols_view_histories d ON d.user_id = b.user_id AND d.alcohol_id = :duplicate
        SET b.view_at = GREATEST(b.view_at, d.view_at)
        WHERE b.alcohol_id = :base
        """,
        params);
    int deletedViews =
        jdbcTemplate.update(
            """
            DELETE d FROM alcohols_view_histories d
            JOIN alcohols_view_histories b ON b.user_id = d.user_id AND b.alcohol_id = :base
            WHERE d.alcohol_id = :duplicate
            """,
            params);
    int movedViews =
        jdbcTemplate.update(
            "UPDATE alcohols_view_histories SET alcohol_id = :base WHERE alcohol_id = :duplicate",
            params);
    return List.of(
        new AlcoholMergeCount("userHistories", movedHistories, 0),
        new AlcoholMergeCount("viewHistories", movedViews, deletedViews));
  }
}
