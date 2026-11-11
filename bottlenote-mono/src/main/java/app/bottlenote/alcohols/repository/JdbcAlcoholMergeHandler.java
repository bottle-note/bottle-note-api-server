package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcAlcoholMergeHandler implements AlcoholMergeHandler {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public List<AlcoholMergeCount> merge(Long duplicateAlcoholId, Long baseAlcoholId) {
    Map<String, Object> params = Map.of("duplicate", duplicateAlcoholId, "base", baseAlcoholId);
    return List.of(
        mergeTastingTags(params), mergeAttributes(params), mergeCurationKeywords(params));
  }

  private AlcoholMergeCount mergeTastingTags(Map<String, Object> params) {
    int deleted =
        jdbcTemplate.update(
            """
            DELETE d FROM alcohols_tasting_tags d
            JOIN alcohols_tasting_tags b
              ON b.tasting_tag_id = d.tasting_tag_id AND b.alcohol_id = :base
            WHERE d.alcohol_id = :duplicate
            """,
            params);
    int moved =
        jdbcTemplate.update(
            "UPDATE alcohols_tasting_tags SET alcohol_id = :base WHERE alcohol_id = :duplicate",
            params);
    return new AlcoholMergeCount("tastingTags", moved, deleted);
  }

  private AlcoholMergeCount mergeAttributes(Map<String, Object> params) {
    // 값을 하나만 가지는 속성은 기준 위스키에 값이 있으면 삭제하고, 여러 개 가지는 속성은 같은 값만 삭제한다
    int deleted =
        jdbcTemplate.update(
            """
            DELETE d FROM alcohol_attribute_mappings d
            JOIN alcohol_attribute_mappings b
              ON b.attribute_id = d.attribute_id AND b.alcohol_id = :base
            JOIN alcohol_attributes a ON a.id = d.attribute_id
            WHERE d.alcohol_id = :duplicate
              AND (a.allow_multiple_values = FALSE OR b.attribute_value = d.attribute_value)
            """,
            params);
    int moved =
        jdbcTemplate.update(
            """
            UPDATE alcohol_attribute_mappings SET alcohol_id = :base
            WHERE alcohol_id = :duplicate
            """,
            params);
    return new AlcoholMergeCount("attributes", moved, deleted);
  }

  private AlcoholMergeCount mergeCurationKeywords(Map<String, Object> params) {
    int deleted =
        jdbcTemplate.update(
            """
            DELETE d FROM curation_keyword_alcohol_ids d
            JOIN curation_keyword_alcohol_ids b
              ON b.curation_keyword_id = d.curation_keyword_id AND b.alcohol_ids = :base
            WHERE d.alcohol_ids = :duplicate
            """,
            params);
    int moved =
        jdbcTemplate.update(
            """
            UPDATE curation_keyword_alcohol_ids SET alcohol_ids = :base
            WHERE alcohol_ids = :duplicate
            """,
            params);
    return new AlcoholMergeCount("curationKeywords", moved, deleted);
  }
}
