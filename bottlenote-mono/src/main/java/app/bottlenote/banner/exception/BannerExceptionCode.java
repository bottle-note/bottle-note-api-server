package app.bottlenote.banner.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum BannerExceptionCode implements ExceptionCode {
  BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "배너를 찾을 수 없습니다."),
  BANNER_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 배너가 이미 존재합니다."),
  BANNER_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다."),
  BANNER_TARGET_URL_REQUIRED(HttpStatus.BAD_REQUEST, "외부 URL 사용 시 이동 URL은 필수입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  BannerExceptionCode(HttpStatus httpStatus, String message) {
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
