package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioStageRepository {
  List<AlcoholAttributeScenarioStage> findStages(Long versionId);

  <S extends AlcoholAttributeScenarioStage> S save(S stage);

  void deleteAll(Iterable<? extends AlcoholAttributeScenarioStage> stages);
}
