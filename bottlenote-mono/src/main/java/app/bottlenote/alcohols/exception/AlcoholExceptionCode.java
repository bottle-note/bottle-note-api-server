package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum AlcoholExceptionCode implements ExceptionCode {
  ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."),
  REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
  DISTILLERY_NOT_FOUND(HttpStatus.NOT_FOUND, "증류소를 찾을 수 없습니다."),
  ALCOHOL_HAS_REVIEWS(HttpStatus.CONFLICT, "리뷰가 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_HAS_RATINGS(HttpStatus.CONFLICT, "평점이 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 위스키입니다."),
  TASTING_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "테이스팅 태그를 찾을 수 없습니다."),
  TASTING_TAG_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 한글 이름의 태그가 이미 존재합니다."),
  TASTING_TAG_HAS_CHILDREN(HttpStatus.CONFLICT, "자식 태그가 존재하는 태그는 삭제할 수 없습니다."),
  TASTING_TAG_HAS_ALCOHOLS(HttpStatus.CONFLICT, "연결된 위스키가 존재하는 태그는 삭제할 수 없습니다."),
  TASTING_TAG_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 태그를 찾을 수 없습니다."),
  TASTING_TAG_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "태그 계층 구조는 최대 3단계까지 가능합니다."),
  CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션을 찾을 수 없습니다."),
  CURATION_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 큐레이션이 이미 존재합니다."),
  CURATION_ALCOHOL_NOT_INCLUDED(HttpStatus.BAD_REQUEST, "해당 위스키가 큐레이션에 포함되어 있지 않습니다.");

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
