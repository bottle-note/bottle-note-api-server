package app.bottlenote.notification.repository;

import org.hibernate.exception.ConstraintViolationException;

final class NotificationConstraintViolation {
  private NotificationConstraintViolation() {}

  static boolean matches(Throwable failure, String key) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String name = violation.getConstraintName();
        return violation.getErrorCode() == 1062
            && name != null
            && (name.equals(key) || name.endsWith("." + key));
      }
    }
    return false;
  }
}
