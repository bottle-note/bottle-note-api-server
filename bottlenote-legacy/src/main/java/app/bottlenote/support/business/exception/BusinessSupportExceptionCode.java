package app.bottlenote.support.business.exception;

import app.bottlenote.shared.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum BusinessSupportExceptionCode implements ExceptionCode {
  BUSINESS_SUPPORT_NOT_FOUND(HttpStatus.BAD_REQUEST, "비즈니스 지원 요청을 찾을 수 없습니다"),
  BUSINESS_SUPPORT_NOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "비즈니스 지원 요청 수정/삭제 권한이 없습니다"),
  BUSINESS_SUPPORT_DUPLICATE(HttpStatus.BAD_REQUEST, "중복된 비즈니스 지원 요청입니다");

  private final HttpStatus httpStatus;
  private final String message;

  BusinessSupportExceptionCode(HttpStatus httpStatus, String message) {
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
