package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class AccessControlException extends AbstractCustomException {
  public AccessControlException(AccessControlExceptionCode code) {
    super(code);
  }
}
