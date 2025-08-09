package app.bottlenote.support.report.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ReportExceptionCode implements ExceptionCode {
  REPORT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상 유저를 찾을 수 없습니다."),
  ALREADY_REPORTED_USER(HttpStatus.BAD_REQUEST, "이미 신고한 사용자입니다."),
  ALREADY_REPORTED_REVIEW(HttpStatus.BAD_REQUEST, "이미 신고한 리뷰입니다."),
  REPORT_CONTENT_NOT_VALID(HttpStatus.BAD_REQUEST, "신고 내용이 유효하지 않습니다."),
  REPORT_CONTENT_OVERFLOW(HttpStatus.BAD_REQUEST, "신고 내용이 300자를 초과했습니다."),
  REPORT_TYPE_NOT_VALID(HttpStatus.BAD_REQUEST, "신고 타입이 유효하지 않습니다."),
  REPORT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "신고 횟수 제한을 초과했습니다."),
  SELF_REPORT(HttpStatus.BAD_REQUEST, "자신을 신고할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  ReportExceptionCode(HttpStatus httpStatus, String message) {
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
