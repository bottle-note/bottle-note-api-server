package app.bottlenote.alcohols.domain;

import java.util.*;

public interface AlcoholScenarioVersionRepository {
  Optional<AlcoholAttributeScenarioVersion> findById(Long id);

  List<AlcoholAttributeScenarioVersion> findVersions(Long scenarioId);

  <S extends AlcoholAttributeScenarioVersion> S save(S version);

  void claimVersion(AlcoholAttributeScenarioVersion version, long expected);

  void delete(AlcoholAttributeScenarioVersion version);

  List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(
      Collection<Long> attributeIds);
}
