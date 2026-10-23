package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.*;

public interface AlcoholScenarioClaims {
  void claimScenario(AlcoholAttributeScenario scenario, long expected);

  void claimVersion(AlcoholAttributeScenarioVersion version, long expected);
}
