package app.bottlenote.notification.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationExceptionCode implements ExceptionCode {
  DUPLICATE_NOTIFICATION_KEY(HttpStatus.CONFLICT, "동일한 사용자와 알림 키가 이미 저장되어 있습니다."),
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  DUPLICATE_NOTIFICATION_SETTING(HttpStatus.BAD_REQUEST, "같은 알림 발생 액션을 한 요청에 두 번 이상 변경할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  NotificationExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
