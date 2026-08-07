package app.bottlenote.agreement.fixture;

import app.bottlenote.agreement.facade.AgreementFacade;
import java.util.HashMap;
import java.util.Map;

/** 단위 테스트용 AgreementFacade Fake. 사용자별 자격 여부를 설정한다. */
public class FakeAgreementFacade implements AgreementFacade {

  private final Map<Long, Boolean> eligibilityByUserId = new HashMap<>();
  private boolean defaultEligible = false;

  public FakeAgreementFacade() {}

  public FakeAgreementFacade(boolean defaultEligible) {
    this.defaultEligible = defaultEligible;
  }

  public FakeAgreementFacade setEligible(Long userId, boolean eligible) {
    eligibilityByUserId.put(userId, eligible);
    return this;
  }

  public FakeAgreementFacade setDefaultEligible(boolean defaultEligible) {
    this.defaultEligible = defaultEligible;
    return this;
  }

  @Override
  public boolean isEligible(Long userId) {
    return eligibilityByUserId.getOrDefault(userId, defaultEligible);
  }
}
