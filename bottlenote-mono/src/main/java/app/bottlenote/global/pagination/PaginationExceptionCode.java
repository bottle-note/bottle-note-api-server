package app.bottlenote.global.pagination;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum PaginationExceptionCode implements ExceptionCode {
  INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서가 올바르지 않습니다."),
  CURSOR_CONTEXT_MISMATCH(HttpStatus.BAD_REQUEST, "커서의 조회 조건이 현재 요청과 일치하지 않습니다."),
  CURSOR_EXPIRED(HttpStatus.GONE, "커서가 만료되었습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  PaginationExceptionCode(HttpStatus httpStatus, String message) {
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
