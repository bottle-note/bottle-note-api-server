package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioVersionRepository {
  Optional<AlcoholAttributeScenarioVersion> findVersion(Long id);

  List<AlcoholAttributeScenarioVersion> findVersions(Long scenarioId);

  AlcoholAttributeScenarioVersion saveVersion(AlcoholAttributeScenarioVersion version);

  void claimVersion(AlcoholAttributeScenarioVersion version, long expected);

  void deleteVersion(AlcoholAttributeScenarioVersion version);

  List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(
      Collection<Long> attributeIds);
}
