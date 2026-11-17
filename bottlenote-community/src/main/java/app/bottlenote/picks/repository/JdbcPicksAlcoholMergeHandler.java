package app.bottlenote.picks.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcPicksAlcoholMergeHandler implements AlcoholMergeHandler {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long duplicateAlcoholId, Long baseAlcoholId) {
    Map<String, Object> params = Map.of("duplicate", duplicateAlcoholId, "base", baseAlcoholId);
    // 기준 위스키의 찜이 해제 상태이면 삭제하고 중복 위스키의 유효한 찜을 옮긴다
    int deletedFromBase =
        jdbcTemplate.update(
            """
            DELETE b FROM picks b
            JOIN picks d ON d.user_id = b.user_id AND d.alcohol_id = :duplicate AND d.status = 'PICK'
            WHERE b.alcohol_id = :base AND b.status <> 'PICK'
            """,
            params);
    int deletedFromDuplicate =
        jdbcTemplate.update(
            """
            DELETE d FROM picks d
            JOIN picks b ON b.user_id = d.user_id AND b.alcohol_id = :base
            WHERE d.alcohol_id = :duplicate
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE picks SET alcohol_id = :base WHERE alcohol_id = :duplicate", params);
    return List.of(new AlcoholMergeCount("picks", moved, deletedFromBase + deletedFromDuplicate));
  }
}
