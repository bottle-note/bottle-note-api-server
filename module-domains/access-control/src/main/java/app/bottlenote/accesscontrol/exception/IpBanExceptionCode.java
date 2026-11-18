package app.bottlenote.accesscontrol.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum IpBanExceptionCode implements ExceptionCode {
  INVALID_IP(HttpStatus.BAD_REQUEST, "유효하지 않은 IP 주소입니다."),
  INVALID_TTL(HttpStatus.BAD_REQUEST, "차단 TTL은 1초 이상 30일 이하여야 합니다."),
  INVALID_REASON(HttpStatus.BAD_REQUEST, "차단 사유는 비어 있을 수 없으며 200자 이하여야 합니다."),
  INVALID_SECURITY_SIGNAL(HttpStatus.BAD_REQUEST, "IP 보안 signal 값이 유효하지 않습니다."),
  IP_SECURITY_SIGNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "IP 보안 signal을 찾을 수 없습니다."),
  IP_SECURITY_SIGNAL_ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 판정된 IP 보안 signal입니다."),
  IP_BAN_NOT_FOUND(HttpStatus.NOT_FOUND, "IP 차단 정보를 찾을 수 없습니다."),
  IP_BAN_NOT_ACTIVE(HttpStatus.CONFLICT, "활성 차단 상태가 아닙니다.");

  private final HttpStatus httpStatus;
  private final String message;

  IpBanExceptionCode(HttpStatus httpStatus, String message) {
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
