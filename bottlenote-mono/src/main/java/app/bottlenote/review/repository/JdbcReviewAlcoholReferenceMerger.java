package app.bottlenote.review.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcReviewAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params = Map.of("source", sourceAlcoholId, "target", targetAlcoholId);
    int moved =
        jdbcTemplate.update(
            "UPDATE reviews SET alcohol_id = :target WHERE alcohol_id = :source", params);
    // 선정 로그의 위스키 ID는 리뷰가 옮겨진 위스키와 같아야 한다
    jdbcTemplate.update(
        "UPDATE best_review_selection_logs SET alcohol_id = :target WHERE alcohol_id = :source",
        params);
    return List.of(new AlcoholMergeCount("reviews", moved, 0));
  }
}
