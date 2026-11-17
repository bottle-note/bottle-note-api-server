package app.bottlenote.global.data.response;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public record Error(ExceptionCode code, HttpStatus status, String message) {
  public static Error of(ExceptionCode code) {
    return new Error(code, code.getHttpStatus(), code.getMessage());
  }
}
