package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioTransitionRepository {
  List<AlcoholAttributeScenarioTransition> findTransitions(Long versionId);

  <S extends AlcoholAttributeScenarioTransition> S save(S transition);

  void deleteAll(Iterable<? extends AlcoholAttributeScenarioTransition> transitions);
}
