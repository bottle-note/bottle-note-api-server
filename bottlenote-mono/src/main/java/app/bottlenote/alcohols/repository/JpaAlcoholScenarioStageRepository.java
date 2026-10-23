package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholScenarioStageRepository
    extends AlcoholScenarioStageRepository, JpaRepository<AlcoholAttributeScenarioStage, Long> {
  @Query(
      "select s from alcohol_attribute_scenario_stage s where s.versionId=:versionId order by s.position")
  List<AlcoholAttributeScenarioStage> findStages(Long versionId);

  default AlcoholAttributeScenarioStage saveStage(AlcoholAttributeScenarioStage value) {
    return save(value);
  }

  default void deleteStages(List<AlcoholAttributeScenarioStage> rows) {
    deleteAll(rows);
  }
}
