package app.bottlenote.shared.common.exception;

import app.bottlenote.shared.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum CommonExceptionCode implements ExceptionCode {
  CONTAINS_PROFANITY(HttpStatus.BAD_REQUEST, "비속어가 포함되어 있습니다."),
  INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 URL입니다."),
  ;

  private final HttpStatus httpStatus;
  private final String message;

  CommonExceptionCode(HttpStatus httpStatus, String message) {
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
