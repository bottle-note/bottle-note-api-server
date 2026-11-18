package app.bottlenote.global.timeseries;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum TimeSeriesExceptionCode implements ExceptionCode {
  INVALID_RANGE(HttpStatus.BAD_REQUEST, "조회 구간이 올바르지 않습니다."),
  RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "조회 구간이 허용 길이를 초과했습니다."),
  UNSUPPORTED_GRANULARITY(HttpStatus.BAD_REQUEST, "지원하지 않는 집계 단위입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  TimeSeriesExceptionCode(HttpStatus httpStatus, String message) {
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
