package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioQuestionRepository {
  List<AlcoholAttributeScenarioQuestion> findQuestions(Long versionId);

  <S extends AlcoholAttributeScenarioQuestion> S save(S question);

  void deleteAll(Iterable<? extends AlcoholAttributeScenarioQuestion> questions);

  boolean hasPublishedAttributeReferences(Collection<Long> attributeIds);

  boolean hasPublishedTastingGroupReferences(Collection<String> codes);
}
