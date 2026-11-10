package app.bottlenote.picks.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcPicksAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params = Map.of("source", sourceAlcoholId, "target", targetAlcoholId);
    // 기준 쪽의 해제된 찜은 중복 쪽 유효한 찜에 자리를 내준다
    int discardedFromTarget =
        jdbcTemplate.update(
            """
            DELETE t FROM picks t
            JOIN picks s ON s.user_id = t.user_id AND s.alcohol_id = :source AND s.status = 'PICK'
            WHERE t.alcohol_id = :target AND t.status <> 'PICK'
            """,
            params);
    int discardedFromSource =
        jdbcTemplate.update(
            """
            DELETE s FROM picks s
            JOIN picks t ON t.user_id = s.user_id AND t.alcohol_id = :target
            WHERE s.alcohol_id = :source
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE picks SET alcohol_id = :target WHERE alcohol_id = :source", params);
    return List.of(
        new AlcoholMergeCount("picks", moved, discardedFromTarget + discardedFromSource));
  }
}
