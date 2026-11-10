package app.bottlenote.mfds.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcMfdsAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params = Map.of("source", sourceAlcoholId, "target", targetAlcoholId);
    // 확정 연결만 옮긴다. 후보와 과거 매칭 기록은 당시 판단의 근거라 그대로 둔다
    int moved =
        jdbcTemplate.update(
            """
            UPDATE mfds_declarations SET selected_alcohol_id = :target
            WHERE selected_alcohol_id = :source
            """,
            params);
    return List.of(new AlcoholMergeCount("mfdsDeclarations", moved, 0));
  }
}
