package app.bottlenote.global.security.jwt;

import app.bottlenote.shared.exception.custom.AbstractCustomException;

public class CustomJwtException extends AbstractCustomException {

  public CustomJwtException(CustomJwtExceptionCode exceptionCode) {
    super(exceptionCode);
  }
}
