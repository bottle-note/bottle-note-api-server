package app.bottlenote.global.validation;

import java.math.BigDecimal;

public final class RatingRangeValidator {

  private static final BigDecimal MIN = new BigDecimal("0.5");
  private static final BigDecimal MAX = new BigDecimal("5.0");
  private static final BigDecimal STEP = new BigDecimal("0.5");

  private RatingRangeValidator() {}

  public static boolean isValid(BigDecimal from, BigDecimal to) {
    return isValidBoundary(from)
        && isValidBoundary(to)
        && (from == null || to == null || from.compareTo(to) <= 0);
  }

  private static boolean isValidBoundary(BigDecimal value) {
    return value == null
        || (value.compareTo(MIN) >= 0
            && value.compareTo(MAX) <= 0
            && value.remainder(STEP).compareTo(BigDecimal.ZERO) == 0);
  }
}
