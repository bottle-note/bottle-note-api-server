package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioQuestionRepository {
  List<AlcoholAttributeScenarioQuestion> findQuestions(Long versionId);

  AlcoholAttributeScenarioQuestion saveQuestion(AlcoholAttributeScenarioQuestion question);

  void deleteQuestions(List<AlcoholAttributeScenarioQuestion> questions);

  boolean hasPublishedAttributeReferences(Collection<Long> attributeIds);

  boolean hasPublishedTastingGroupReferences(Collection<String> codes);
}
