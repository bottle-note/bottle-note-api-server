package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholScenarioQuestionRepository
    extends AlcoholScenarioQuestionRepository,
        JpaRepository<AlcoholAttributeScenarioQuestion, Long>,
        AlcoholScenarioQuestionReferences {
  @Query(
      "select q from alcohol_attribute_scenario_question q where q.versionId=:versionId order by q.stageId,q.branchOrder")
  List<AlcoholAttributeScenarioQuestion> findQuestions(Long versionId);

  @Query(
      "select count(q)>0 from alcohol_attribute_scenario_question q, alcohol_attribute_scenario_version v where q.versionId=v.id and v.status=app.bottlenote.alcohols.constant.ScenarioTypes$Status.PUBLISHED and q.attributeId in :attributeIds")
  boolean hasPublishedAttributeReferences(Collection<Long> attributeIds);
}
