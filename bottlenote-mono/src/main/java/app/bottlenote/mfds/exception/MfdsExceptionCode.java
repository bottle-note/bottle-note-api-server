package app.bottlenote.mfds.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum MfdsExceptionCode implements ExceptionCode {
  MFDS_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "수입 신고 원장을 찾을 수 없습니다."),
  MFDS_IMPORTER_NOT_FOUND(HttpStatus.NOT_FOUND, "수입사를 찾을 수 없습니다."),
  MFDS_IMPORTER_DUPLICATE_CODE(HttpStatus.CONFLICT, "동일한 공식 업소 코드의 수입사가 이미 존재합니다."),
  MFDS_IMPORTER_HAS_DECLARATIONS(HttpStatus.CONFLICT, "연결된 수입 신고가 있어 수입사를 삭제할 수 없습니다."),
  MFDS_IMPORTER_HAS_RCNO_LINKS(HttpStatus.CONFLICT, "연결된 수입신고번호 근거가 있어 수입사를 삭제할 수 없습니다."),
  MFDS_DECLARATION_NOT_FOUND(HttpStatus.NOT_FOUND, "수입 신고 데이터를 찾을 수 없습니다."),
  MFDS_DECLARATION_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 수입사가 연결된 수입 신고입니다. 먼저 연결을 해제해 주세요."),
  MFDS_DECLARATION_NOT_LINKED(HttpStatus.BAD_REQUEST, "수입사가 연결되지 않은 수입 신고입니다."),
  MFDS_RCNO_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "수입신고번호 연결 근거를 찾을 수 없습니다."),
  MFDS_RCNO_LINK_DUPLICATE(HttpStatus.CONFLICT, "해당 수입신고번호에 이미 연결 근거가 존재합니다."),
  MFDS_SELECTED_ALCOHOL_NOT_FOUND(HttpStatus.BAD_REQUEST, "선택한 주류가 존재하지 않습니다."),
  MFDS_SELECTED_DISTILLERY_NOT_FOUND(HttpStatus.BAD_REQUEST, "선택한 증류소가 존재하지 않습니다."),
  MFDS_SELECTED_REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "선택한 지역이 존재하지 않습니다."),
  MFDS_PRODUCT_IDENTITY_UNAVAILABLE(
      HttpStatus.BAD_REQUEST, "제품 식별 키가 없어 일괄 확정할 수 없습니다. 이 신고는 기존 단건 확정을 사용해 주세요."),
  MFDS_BULK_SELECTION_LIMIT(HttpStatus.BAD_REQUEST, "한 번에 확정할 수 있는 신고는 500건까지입니다."),
  MFDS_BULK_PREVIEW_EXPIRED(HttpStatus.CONFLICT, "미리보기 유효 시간이 지났습니다. 다시 조회해 주세요."),
  MFDS_BULK_PREVIEW_MISMATCH(HttpStatus.CONFLICT, "미리보기 이후 신고나 기준 정보가 바뀌었습니다. 다시 조회해 주세요."),
  MFDS_BULK_TARGET_INVALID(HttpStatus.BAD_REQUEST, "미리보기에서 적용할 수 없는 신고가 포함되어 있습니다."),
  MFDS_BULK_DUPLICATE_TARGET(HttpStatus.BAD_REQUEST, "같은 신고가 요청에 중복되어 있습니다."),
  MFDS_BULK_EMPTY_SELECTION(HttpStatus.BAD_REQUEST, "적용할 신고를 한 건 이상 선택해 주세요."),
  MFDS_BULK_PREVIEW_NOT_ISSUED(HttpStatus.CONFLICT, "서버가 발급한 미리보기가 아닙니다. 다시 조회해 주세요."),
  MFDS_BULK_PREVIEW_ADMIN_MISMATCH(HttpStatus.FORBIDDEN, "미리보기를 발급한 관리자만 확정할 수 있습니다."),
  MFDS_BULK_PREVIEW_STORE_UNAVAILABLE(
      HttpStatus.SERVICE_UNAVAILABLE, "미리보기 발급 저장소를 사용하지 못했습니다. 다시 조회해 주세요.");

  private final HttpStatus httpStatus;
  private final String message;

  MfdsExceptionCode(HttpStatus httpStatus, String message) {
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
