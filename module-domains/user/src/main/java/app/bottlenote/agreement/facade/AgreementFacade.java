package app.bottlenote.agreement.facade;

/** 다른 도메인에 사용자의 약관 동의 자격 판정을 제공한다. */
public interface AgreementFacade {

  boolean isEligible(Long userId);
}
