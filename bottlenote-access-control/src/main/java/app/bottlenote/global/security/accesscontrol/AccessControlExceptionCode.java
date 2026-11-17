package app.bottlenote.global.security.accesscontrol;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum AccessControlExceptionCode implements ExceptionCode {
  RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
  IP_BANNED(HttpStatus.FORBIDDEN, "접근이 일시적으로 제한된 IP입니다."),
  INVALID_IP(HttpStatus.BAD_REQUEST, "유효하지 않은 IP 주소입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  AccessControlExceptionCode(HttpStatus httpStatus, String message) {
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
