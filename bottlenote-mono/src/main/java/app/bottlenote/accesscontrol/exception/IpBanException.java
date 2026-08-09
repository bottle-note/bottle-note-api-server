package app.bottlenote.accesscontrol.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class IpBanException extends AbstractCustomException {
  public IpBanException(IpBanExceptionCode code) {
    super(code);
  }
}
