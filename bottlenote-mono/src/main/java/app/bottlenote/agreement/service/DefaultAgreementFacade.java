package app.bottlenote.agreement.service;

import app.bottlenote.agreement.facade.AgreementFacade;
import app.bottlenote.common.annotation.FacadeService;
import lombok.RequiredArgsConstructor;

/** 사용자 약관 동의 자격 판정 파사드의 기본 구현체다. */
@FacadeService
@RequiredArgsConstructor
public class DefaultAgreementFacade implements AgreementFacade {

  private final AgreementEvaluator agreementEvaluator;

  @Override
  public boolean isEligible(Long userId) {
    return agreementEvaluator.evaluate(userId).eligible();
  }
}
