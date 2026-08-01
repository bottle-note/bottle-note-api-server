package app.bottlenote.agreement.service;

import app.bottlenote.agreement.facade.AgreementFacade;
import app.bottlenote.common.annotation.FacadeService;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultAgreementFacade implements AgreementFacade {

  private final AgreementEvaluator agreementEvaluator;

  @Override
  public boolean isEligible(Long userId) {
    return agreementEvaluator.evaluate(userId).eligible();
  }
}
