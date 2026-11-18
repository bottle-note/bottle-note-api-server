package app.bottlenote.agreement.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

/** 사용자 약관 동의 처리 중 발생한 예외를 나타낸다. */
public class AgreementException extends AbstractCustomException {

  public AgreementException(AgreementExceptionCode code) {
    super(code);
  }
}
