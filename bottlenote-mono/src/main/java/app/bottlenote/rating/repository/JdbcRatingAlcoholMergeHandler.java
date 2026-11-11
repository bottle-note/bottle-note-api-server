package app.bottlenote.rating.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcRatingAlcoholMergeHandler implements AlcoholMergeHandler {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long duplicateAlcoholId, Long baseAlcoholId) {
    Map<String, Object> params = Map.of("duplicate", duplicateAlcoholId, "base", baseAlcoholId);
    // 기준 위스키의 별점이 0점(지운 별점)이면 삭제하고 중복 위스키의 유효한 별점을 옮긴다
    int deletedFromBase =
        jdbcTemplate.update(
            """
            DELETE b FROM ratings b
            JOIN ratings d ON d.user_id = b.user_id AND d.alcohol_id = :duplicate AND d.rating > 0
            WHERE b.alcohol_id = :base AND b.rating = 0
            """,
            params);
    int deletedFromDuplicate =
        jdbcTemplate.update(
            """
            DELETE d FROM ratings d
            JOIN ratings b ON b.user_id = d.user_id AND b.alcohol_id = :base
            WHERE d.alcohol_id = :duplicate
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE ratings SET alcohol_id = :base WHERE alcohol_id = :duplicate", params);
    return List.of(new AlcoholMergeCount("ratings", moved, deletedFromBase + deletedFromDuplicate));
  }
}
