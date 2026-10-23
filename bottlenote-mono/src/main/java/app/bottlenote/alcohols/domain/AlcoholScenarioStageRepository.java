package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioStageRepository {
  List<AlcoholAttributeScenarioStage> findStages(Long versionId);

  AlcoholAttributeScenarioStage saveStage(AlcoholAttributeScenarioStage stage);

  void deleteStages(List<AlcoholAttributeScenarioStage> stages);
}
