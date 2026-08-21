package app.bottlenote.mfds.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum MfdsExceptionCode implements ExceptionCode {
  MFDS_IMPORTER_NOT_FOUND(HttpStatus.NOT_FOUND, "수입사를 찾을 수 없습니다."),
  MFDS_IMPORTER_DUPLICATE_CODE(HttpStatus.CONFLICT, "동일한 공식 업소 코드의 수입사가 이미 존재합니다."),
  MFDS_IMPORTER_HAS_DECLARATIONS(HttpStatus.CONFLICT, "연결된 수입 신고가 있어 수입사를 삭제할 수 없습니다."),
  MFDS_IMPORTER_HAS_RCNO_LINKS(HttpStatus.CONFLICT, "연결된 수입신고번호 근거가 있어 수입사를 삭제할 수 없습니다."),
  MFDS_DECLARATION_NOT_FOUND(HttpStatus.NOT_FOUND, "수입 신고 데이터를 찾을 수 없습니다."),
  MFDS_DECLARATION_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 수입사가 연결된 수입 신고입니다. 먼저 연결을 해제해 주세요."),
  MFDS_DECLARATION_NOT_LINKED(HttpStatus.BAD_REQUEST, "수입사가 연결되지 않은 수입 신고입니다."),
  MFDS_RCNO_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "수입신고번호 연결 근거를 찾을 수 없습니다."),
  MFDS_RCNO_LINK_DUPLICATE(HttpStatus.CONFLICT, "해당 수입신고번호에 이미 연결 근거가 존재합니다.");

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
