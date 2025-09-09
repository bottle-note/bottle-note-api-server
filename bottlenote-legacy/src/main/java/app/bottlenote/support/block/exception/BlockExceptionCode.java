package app.bottlenote.support.block.exception;

import app.bottlenote.shared.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum BlockExceptionCode implements ExceptionCode {
  USER_ALREADY_BLOCKED(HttpStatus.CONFLICT, "이미 차단된 사용자입니다."),
  USER_BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "차단 관계를 찾을 수 없습니다."),
  CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),
  REQUIRED_USER_ID(HttpStatus.BAD_REQUEST, "유저 아이디가 필요합니다.");

  private final HttpStatus httpStatus;
  private final String message;

  BlockExceptionCode(HttpStatus httpStatus, String message) {
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
