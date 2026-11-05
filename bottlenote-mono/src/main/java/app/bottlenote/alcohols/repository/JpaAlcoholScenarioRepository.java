package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.SCENARIO_CODE_OCCUPIED;
import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.SCENARIO_SLOT_OCCUPIED;

import app.bottlenote.alcohols.constant.ScenarioTypes.Status;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaAlcoholScenarioRepository
    extends AlcoholScenarioRepository,
        JpaRepository<AlcoholAttributeScenario, Long>,
        AlcoholScenarioClaims {
  @Query("select s from alcohol_attribute_scenario s where s.groupId=:groupId order by s.slot")
  List<AlcoholAttributeScenario> findScenarios(Long groupId);

  @Query(
      "select s from alcohol_attribute_scenario s where s.enabled = true and exists (select v.id from alcohol_attribute_scenario_version v where v.id = s.activeVersionId and v.scenarioId = s.id and v.status = :status) order by s.id")
  List<AlcoholAttributeScenario> findPublishedScenarios(Status status);

  @Query("select count(s)>0 from alcohol_attribute_scenario s where s.groupId=:groupId")
  boolean hasScenarios(Long groupId);

  default AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario value) {
    try {
      return saveAndFlush(value);
    } catch (DataIntegrityViolationException ex) {
      if (AlcoholAttributeConstraintViolation.isDuplicate(ex, Set.of("uk_scenarios_group_slot")))
        throw new ScenarioException(SCENARIO_SLOT_OCCUPIED);
      if (AlcoholAttributeConstraintViolation.isDuplicate(ex, Set.of("uk_scenarios_code")))
        throw new ScenarioException(SCENARIO_CODE_OCCUPIED);
      throw ex;
    }
  }
}
