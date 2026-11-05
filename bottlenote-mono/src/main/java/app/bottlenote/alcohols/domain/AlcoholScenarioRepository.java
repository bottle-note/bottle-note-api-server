package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.Status;
import java.util.*;

public interface AlcoholScenarioRepository {
  List<AlcoholAttributeScenario> findScenarios(Long groupId);

  List<AlcoholAttributeScenario> findPublishedScenarios(Status status);

  Optional<AlcoholAttributeScenario> findById(Long id);

  AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario scenario);

  void claimScenario(AlcoholAttributeScenario scenario, long expected);

  void delete(AlcoholAttributeScenario scenario);

  boolean hasScenarios(Long groupId);

  void flush();
}
