package app.external.notification.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class NotificationException extends AbstractCustomException {

  public NotificationException(NotificationExceptionCode code) {
    super(code);
  }
}
