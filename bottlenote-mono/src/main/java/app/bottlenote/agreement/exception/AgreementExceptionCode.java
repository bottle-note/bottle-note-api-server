package app.bottlenote.agreement.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 사용자 약관 동의 처리에 사용하는 예외 코드를 정의한다. */
@Getter
public enum AgreementExceptionCode implements ExceptionCode {
  INVALID_AGREEMENT_TYPE(HttpStatus.BAD_REQUEST, "알 수 없는 동의 유형입니다."),
  UNSUPPORTED_AGREEMENT_ACTION(HttpStatus.BAD_REQUEST, "요청할 수 없는 동의 상태입니다."),
  AGREEMENT_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "동의 문서 원문은 비어 있을 수 없습니다."),
  AGREEMENT_REQUIRED(HttpStatus.FORBIDDEN, "필수 약관 동의가 필요합니다.");

  private final HttpStatus httpStatus;
  private final String message;

  AgreementExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
