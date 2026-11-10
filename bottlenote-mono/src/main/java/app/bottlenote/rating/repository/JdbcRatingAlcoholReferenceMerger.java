package app.bottlenote.rating.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcRatingAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params = Map.of("source", sourceAlcoholId, "target", targetAlcoholId);
    // 기준 쪽의 지워진 별점(0점)은 중복 쪽 실제 별점에 자리를 내준다
    int discardedFromTarget =
        jdbcTemplate.update(
            """
            DELETE t FROM ratings t
            JOIN ratings s ON s.user_id = t.user_id AND s.alcohol_id = :source AND s.rating > 0
            WHERE t.alcohol_id = :target AND t.rating = 0
            """,
            params);
    int discardedFromSource =
        jdbcTemplate.update(
            """
            DELETE s FROM ratings s
            JOIN ratings t ON t.user_id = s.user_id AND t.alcohol_id = :target
            WHERE s.alcohol_id = :source
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE ratings SET alcohol_id = :target WHERE alcohol_id = :source", params);
    return List.of(
        new AlcoholMergeCount("ratings", moved, discardedFromTarget + discardedFromSource));
  }
}
