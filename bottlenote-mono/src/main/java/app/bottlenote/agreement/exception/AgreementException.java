package app.bottlenote.agreement.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class AgreementException extends AbstractCustomException {

  public AgreementException(AgreementExceptionCode code) {
    super(code);
  }
}
