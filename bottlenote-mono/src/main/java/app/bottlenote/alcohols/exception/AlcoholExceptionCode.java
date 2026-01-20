package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum AlcoholExceptionCode implements ExceptionCode {
  ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."),
  REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
  DISTILLERY_NOT_FOUND(HttpStatus.NOT_FOUND, "증류소를 찾을 수 없습니다."),
  ALCOHOL_HAS_REVIEWS(HttpStatus.CONFLICT, "리뷰가 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_HAS_RATINGS(HttpStatus.CONFLICT, "평점이 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 위스키입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  AlcoholExceptionCode(HttpStatus httpStatus, String message) {
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
