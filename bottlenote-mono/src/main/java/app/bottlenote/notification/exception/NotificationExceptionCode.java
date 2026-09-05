package app.bottlenote.notification.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationExceptionCode implements ExceptionCode {
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  INVALID_NOTIFICATION_PREFERENCE(HttpStatus.BAD_REQUEST, "알림 수신 설정이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  NotificationExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
