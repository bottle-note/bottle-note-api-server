package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum AlcoholExceptionCode implements ExceptionCode {
  ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."),
  REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
  DISTILLERY_NOT_FOUND(HttpStatus.NOT_FOUND, "증류소를 찾을 수 없습니다."),
  DISTILLERY_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 증류소가 이미 존재합니다."),
  DISTILLERY_HAS_ALCOHOLS(HttpStatus.CONFLICT, "연관된 위스키가 존재하는 증류소는 삭제할 수 없습니다."),
  ALCOHOL_HAS_REVIEWS(HttpStatus.CONFLICT, "리뷰가 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_HAS_RATINGS(HttpStatus.CONFLICT, "평점이 존재하는 위스키는 삭제할 수 없습니다."),
  ALCOHOL_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 위스키입니다."),
  TASTING_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "테이스팅 태그를 찾을 수 없습니다."),
  TASTING_TAG_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 한글 이름의 태그가 이미 존재합니다."),
  TASTING_TAG_HAS_CHILDREN(HttpStatus.CONFLICT, "자식 태그가 존재하는 태그는 삭제할 수 없습니다."),
  TASTING_TAG_HAS_ALCOHOLS(HttpStatus.CONFLICT, "연결된 위스키가 존재하는 태그는 삭제할 수 없습니다."),
  TASTING_TAG_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 태그를 찾을 수 없습니다."),
  TASTING_TAG_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "태그 계층 구조는 최대 3단계까지 가능합니다."),
  TASTING_TAG_MAPPING_DUPLICATE(HttpStatus.CONFLICT, "이미 연결된 주류-태그 매핑입니다."),
  CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션을 찾을 수 없습니다."),
  CURATION_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 큐레이션이 이미 존재합니다."),
  CURATION_ALCOHOL_NOT_INCLUDED(HttpStatus.BAD_REQUEST, "해당 위스키가 큐레이션에 포함되어 있지 않습니다."),
  CURATION_REORDER_DUPLICATE_ID(HttpStatus.BAD_REQUEST, "중복된 큐레이션 ID로 정렬을 요청할 수 없습니다."),
  DISTILLERY_REORDER_DUPLICATE_ID(HttpStatus.BAD_REQUEST, "중복된 증류소 ID로 정렬을 요청할 수 없습니다."),
  REGION_DUPLICATE_KOR_NAME(HttpStatus.CONFLICT, "동일한 한글 이름의 지역이 이미 존재합니다."),
  REGION_DUPLICATE_ENG_NAME(HttpStatus.CONFLICT, "동일한 영문 이름의 지역이 이미 존재합니다."),
  REGION_HAS_CHILDREN(HttpStatus.CONFLICT, "자식 지역이 존재하는 지역은 삭제할 수 없습니다."),
  REGION_HAS_ALCOHOLS(HttpStatus.CONFLICT, "연결된 위스키가 존재하는 지역은 삭제할 수 없습니다."),
  REGION_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 지역을 찾을 수 없습니다."),
  REGION_PARENT_CYCLE(HttpStatus.BAD_REQUEST, "자기 자신 또는 하위 지역을 부모로 지정할 수 없습니다."),
  REGION_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "지역 계층 구조는 최대 2단계까지 가능합니다."),
  REGION_REORDER_DUPLICATE_ID(HttpStatus.BAD_REQUEST, "중복된 지역 ID로 정렬을 요청할 수 없습니다."),
  REGION_REORDER_SCOPE_MISMATCH(HttpStatus.BAD_REQUEST, "부모 지역이 다른 지역은 함께 정렬할 수 없습니다."),
  ALCOHOL_LOOKUP_UNAVAILABLE(
      HttpStatus.SERVICE_UNAVAILABLE, "위스키 조회를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."),
  EXCEL_INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "OOXML .xlsx 파일만 업로드할 수 있습니다."),
  EXCEL_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "엑셀 파일 크기는 5MiB를 초과할 수 없습니다."),
  EXCEL_SHEET_NOT_FOUND(HttpStatus.BAD_REQUEST, "필수 시트가 없거나 시트명이 올바르지 않습니다."),
  EXCEL_HEADER_MISMATCH(HttpStatus.BAD_REQUEST, "엑셀 헤더(1행)가 고정 템플릿과 일치하지 않습니다."),
  EXCEL_DESCRIPTION_MISMATCH(HttpStatus.BAD_REQUEST, "엑셀 설명(2행)이 고정 템플릿과 일치하지 않습니다."),
  EXCEL_DUPLICATE_HEADER(HttpStatus.BAD_REQUEST, "엑셀 헤더에 중복된 필드명이 있습니다."),
  EXCEL_FORMULA_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "수식 셀은 허용되지 않습니다."),
  EXCEL_EXTERNAL_LINK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "외부 링크는 허용되지 않습니다."),
  EXCEL_ROW_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "데이터 행은 최대 1,000행까지 검증할 수 있습니다.");

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
