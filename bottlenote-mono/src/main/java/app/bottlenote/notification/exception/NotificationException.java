package app.bottlenote.notification.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class NotificationException extends AbstractCustomException {

  public NotificationException(NotificationExceptionCode code) {
    super(code);
  }

  public NotificationException(NotificationExceptionCode code, Throwable cause) {
    super(code);
    initCause(cause);
  }
}
