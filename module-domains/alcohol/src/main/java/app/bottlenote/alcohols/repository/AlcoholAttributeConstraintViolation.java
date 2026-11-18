package app.bottlenote.alcohols.repository;

import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;

final class AlcoholAttributeConstraintViolation {
  private static final Set<String> UNIQUE_KEYS =
      Set.of("uk_alcohol_attributes_key", "uk_alcohol_attributes_display_name");

  private AlcoholAttributeConstraintViolation() {}

  static boolean isDuplicate(Throwable failure) {
    return isDuplicate(failure, UNIQUE_KEYS);
  }

  static boolean isDuplicate(Throwable failure, Set<String> keys) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String name = violation.getConstraintName();
        return violation.getErrorCode() == 1062
            && name != null
            && keys.stream().anyMatch(key -> name.equals(key) || name.endsWith("." + key));
      }
    }
    return false;
  }
}
