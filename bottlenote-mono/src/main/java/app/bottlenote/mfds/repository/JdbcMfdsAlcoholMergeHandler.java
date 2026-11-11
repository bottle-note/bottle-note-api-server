package app.bottlenote.mfds.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcMfdsAlcoholMergeHandler implements AlcoholMergeHandler {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long duplicateAlcoholId, Long baseAlcoholId) {
    Map<String, Object> params = Map.of("duplicate", duplicateAlcoholId, "base", baseAlcoholId);
    // 확정된 연결만 옮긴다. 후보와 과거 매칭 기록은 당시 판단의 근거이므로 바꾸지 않는다
    int moved =
        jdbcTemplate.update(
            """
            UPDATE mfds_declarations SET selected_alcohol_id = :base
            WHERE selected_alcohol_id = :duplicate
            """,
            params);
    return List.of(new AlcoholMergeCount("mfdsDeclarations", moved, 0));
  }
}
