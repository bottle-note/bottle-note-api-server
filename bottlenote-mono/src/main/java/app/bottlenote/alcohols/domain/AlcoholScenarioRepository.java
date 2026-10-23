package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioRepository {
  List<AlcoholAttributeScenario> findScenarios(Long groupId);

  Optional<AlcoholAttributeScenario> findScenario(Long id);

  Optional<AlcoholAttributeScenarioVersion> findVersion(Long id);

  List<AlcoholAttributeScenarioVersion> findVersions(Long scenarioId);

  List<AlcoholAttributeScenarioStage> findStages(Long versionId);

  List<AlcoholAttributeScenarioQuestion> findQuestions(Long versionId);

  List<AlcoholAttributeScenarioTransition> findTransitions(Long versionId);

  AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario scenario);

  AlcoholAttributeScenarioVersion saveVersion(AlcoholAttributeScenarioVersion version);

  AlcoholAttributeScenarioStage saveStage(AlcoholAttributeScenarioStage stage);

  AlcoholAttributeScenarioQuestion saveQuestion(AlcoholAttributeScenarioQuestion question);

  AlcoholAttributeScenarioTransition saveTransition(AlcoholAttributeScenarioTransition transition);

  void claimScenario(AlcoholAttributeScenario scenario, long expected);

  void claimVersion(AlcoholAttributeScenarioVersion version, long expected);

  void deleteScenario(AlcoholAttributeScenario scenario);

  void deleteVersion(AlcoholAttributeScenarioVersion version);

  void deleteStages(List<AlcoholAttributeScenarioStage> stages);

  void deleteQuestions(List<AlcoholAttributeScenarioQuestion> questions);

  void deleteTransitions(List<AlcoholAttributeScenarioTransition> transitions);

  boolean hasScenarios(Long groupId);

  boolean hasPublishedAttributeReferences(Collection<Long> attributeIds);

  List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(
      Collection<Long> attributeIds);

  List<TastingTagGroup> lockTastingGroups(Collection<String> codes);

  void lockTastingTags(Collection<Long> ids);

  List<String> findTastingGroupCodesByTagId(Long tagId);

  boolean hasPublishedTastingGroupReference(String code);

  void flush();
}
