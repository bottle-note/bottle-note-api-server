package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcAlcoholReferenceMerger implements AlcoholReferenceMerger {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId) {
    Map<String, Object> params = Map.of("source", sourceAlcoholId, "target", targetAlcoholId);
    return List.of(
        mergeTastingTags(params), mergeAttributes(params), mergeCurationKeywords(params));
  }

  private AlcoholMergeCount mergeTastingTags(Map<String, Object> params) {
    int discarded =
        jdbcTemplate.update(
            """
            DELETE s FROM alcohols_tasting_tags s
            JOIN alcohols_tasting_tags t
              ON t.tasting_tag_id = s.tasting_tag_id AND t.alcohol_id = :target
            WHERE s.alcohol_id = :source
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE alcohols_tasting_tags SET alcohol_id = :target WHERE alcohol_id = :source",
            params);
    return new AlcoholMergeCount("tastingTags", moved, discarded);
  }

  private AlcoholMergeCount mergeAttributes(Map<String, Object> params) {
    // 값을 하나만 가지는 속성은 기준 위스키에 값이 있으면 삭제하고, 여러 개 가지는 속성은 같은 값만 삭제한다
    int discarded =
        jdbcTemplate.update(
            """
            DELETE s FROM alcohol_attribute_mappings s
            JOIN alcohol_attribute_mappings t
              ON t.attribute_id = s.attribute_id AND t.alcohol_id = :target
            JOIN alcohol_attributes a ON a.id = s.attribute_id
            WHERE s.alcohol_id = :source
              AND (a.allow_multiple_values = FALSE OR t.attribute_value = s.attribute_value)
            """,
            params);
    int moved =
        jdbcTemplate.update(
            """
            UPDATE alcohol_attribute_mappings SET alcohol_id = :target
            WHERE alcohol_id = :source
            """,
            params);
    return new AlcoholMergeCount("attributes", moved, discarded);
  }

  private AlcoholMergeCount mergeCurationKeywords(Map<String, Object> params) {
    int discarded =
        jdbcTemplate.update(
            """
            DELETE s FROM curation_keyword_alcohol_ids s
            JOIN curation_keyword_alcohol_ids t
              ON t.curation_keyword_id = s.curation_keyword_id AND t.alcohol_ids = :target
            WHERE s.alcohol_ids = :source
            """,
            params);
    int moved =
        jdbcTemplate.update(
            """
            UPDATE curation_keyword_alcohol_ids SET alcohol_ids = :target
            WHERE alcohol_ids = :source
            """,
            params);
    return new AlcoholMergeCount("curationKeywords", moved, discarded);
  }
}
