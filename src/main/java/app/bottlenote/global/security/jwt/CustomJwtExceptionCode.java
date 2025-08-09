package app.bottlenote.global.security.jwt;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum CustomJwtExceptionCode implements ExceptionCode {
  EMPTY_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다");

  private final HttpStatus httpStatus;
  private final String message;

  CustomJwtExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
