package app.bottlenote.common.file.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum FileExceptionCode implements ExceptionCode {
  EXPIRY_TIME_RANGE_INVALID(HttpStatus.BAD_REQUEST, "만료 기간의 범위가 적절하지 않습니다.( 최소 1분 ,최대 10분) "),
  UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 Content-Type입니다."),
  INVALID_RESOURCE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 리소스 URL입니다."),
  RESOURCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "등록되지 않은 리소스입니다."),
  RESOURCE_OWNER_MISMATCH(HttpStatus.BAD_REQUEST, "리소스 소유자가 일치하지 않습니다."),
  RESOURCE_ALREADY_USED(HttpStatus.BAD_REQUEST, "사용할 수 없는 리소스 상태입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  FileExceptionCode(HttpStatus httpStatus, String message) {
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
