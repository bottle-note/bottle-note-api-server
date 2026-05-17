package app.bottlenote.curation.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum CurationExceptionCode implements ExceptionCode {
  CURATION_SPEC_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션 스펙을 찾을 수 없습니다."),
  CURATION_SPEC_DUPLICATE_CODE(HttpStatus.CONFLICT, "동일한 큐레이션 스펙 코드가 이미 존재합니다."),
  CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션을 찾을 수 없습니다."),
  CURATION_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "큐레이션 payload가 스펙과 일치하지 않습니다."),
  CURATION_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "큐레이션 응답이 스펙과 일치하지 않습니다."),
  CURATION_GRAPHQL_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "큐레이션 GraphQL 보강에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  CurationExceptionCode(HttpStatus httpStatus, String message) {
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
