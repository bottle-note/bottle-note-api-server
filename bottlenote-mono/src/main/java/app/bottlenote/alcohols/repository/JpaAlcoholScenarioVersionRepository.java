package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholScenarioVersionRepository
    extends AlcoholScenarioVersionRepository,
        JpaRepository<AlcoholAttributeScenarioVersion, Long>,
        AlcoholScenarioClaims {
  default Optional<AlcoholAttributeScenarioVersion> findVersion(Long id) {
    return findById(id);
  }

  @Query(
      "select v from alcohol_attribute_scenario_version v where v.scenarioId=:scenarioId order by v.versionNo")
  List<AlcoholAttributeScenarioVersion> findVersions(Long scenarioId);

  @Query(
      "select distinct v from alcohol_attribute_scenario_version v, alcohol_attribute_scenario_question q where q.versionId=v.id and v.status=app.bottlenote.alcohols.constant.ScenarioTypes$Status.DRAFT and q.attributeId in :attributeIds order by v.id")
  List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(
      Collection<Long> attributeIds);

  default AlcoholAttributeScenarioVersion saveVersion(AlcoholAttributeScenarioVersion value) {
    return save(value);
  }

  default void deleteVersion(AlcoholAttributeScenarioVersion value) {
    delete(value);
  }
}
