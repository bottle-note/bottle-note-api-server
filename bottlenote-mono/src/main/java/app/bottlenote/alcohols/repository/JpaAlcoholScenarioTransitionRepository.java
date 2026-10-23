package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholScenarioTransitionRepository
    extends AlcoholScenarioTransitionRepository,
        JpaRepository<AlcoholAttributeScenarioTransition, Long> {
  @Query(
      "select t from alcohol_attribute_scenario_transition t where t.versionId=:versionId order by t.fromQuestionId,t.priority")
  List<AlcoholAttributeScenarioTransition> findTransitions(Long versionId);

  default AlcoholAttributeScenarioTransition saveTransition(
      AlcoholAttributeScenarioTransition value) {
    return save(value);
  }

  default void deleteTransitions(List<AlcoholAttributeScenarioTransition> rows) {
    deleteAll(rows);
  }
}
