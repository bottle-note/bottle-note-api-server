package app.bottlenote.global.security.jwt;

import app.bottlenote.shared.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum JwtExceptionType implements ExceptionCode {
  INVALID_SIGNATURE("잘못된 JWT 서명입니다.", HttpStatus.UNAUTHORIZED),
  MALFORMED_TOKEN("잘못된 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
  EXPIRED_TOKEN("만료된 JWT 토큰입니다.", HttpStatus.FORBIDDEN),
  UNSUPPORTED_TOKEN("지원되지 않는 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
  ILLEGAL_ARGUMENT("잘못 된 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
  UNKNOWN_ERROR("JWT 처리 중 알 수 없는 오류가 발생했습니다.", HttpStatus.UNAUTHORIZED);

  private final String message;
  private final HttpStatus status;

  JwtExceptionType(String message, HttpStatus status) {
    this.message = message;
    this.status = status;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return status;
  }
}
