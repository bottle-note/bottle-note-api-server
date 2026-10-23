package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioTransitionRepository {
  List<AlcoholAttributeScenarioTransition> findTransitions(Long versionId);

  AlcoholAttributeScenarioTransition saveTransition(AlcoholAttributeScenarioTransition transition);

  void deleteTransitions(List<AlcoholAttributeScenarioTransition> transitions);
}
