package app.bottlenote.agreement.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AgreementExceptionCode implements ExceptionCode {
  INVALID_AGREEMENT_TYPE(HttpStatus.BAD_REQUEST, "알 수 없는 동의 유형입니다."),
  AGREEMENT_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "동의 문서 원문은 비어 있을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  AgreementExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
