package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioRepository {
  List<AlcoholAttributeScenario> findScenarios(Long groupId);

  Optional<AlcoholAttributeScenario> findScenario(Long id);

  AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario scenario);

  void claimScenario(AlcoholAttributeScenario scenario, long expected);

  void deleteScenario(AlcoholAttributeScenario scenario);

  boolean hasScenarios(Long groupId);

  void flush();
}
