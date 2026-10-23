package app.bottlenote.alcohols.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlcoholScenarioQuestionReferencesImpl implements AlcoholScenarioQuestionReferences {
  private final EntityManager em;
  private final ObjectMapper mapper;

  public boolean hasPublishedTastingGroupReferences(Collection<String> codes) {
    if (codes.isEmpty()) return false;
    String json = mapper.valueToTree(codes).toString();
    Number found =
        (Number)
            em.createNativeQuery(
                    """
     SELECT EXISTS(SELECT 1 FROM alcohol_attribute_scenario_questions q
       JOIN alcohol_attribute_scenario_versions v ON v.id=q.version_id
       WHERE v.status='PUBLISHED' AND q.source_kind='TASTING_GROUP'
       AND JSON_OVERLAPS(JSON_EXTRACT(q.input_config,'$.choices[*].value'), CAST(:codes AS JSON)))
     """)
                .setParameter("codes", json)
                .getSingleResult();
    return found.intValue() != 0;
  }
}
