package app.bottlenote.campaigncontent.repository;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;

/** 캠페인 콘텐츠 영속성 예외에서 알려진 DB 제약 이름만 식별한다. */
final class CampaignContentConstraintViolationClassifier {

  private static final Pattern MYSQL_CONSTRAINT_IDENTIFIER =
      Pattern.compile(
          "(?i)\\bCONSTRAINT\\s+((?:`[^`]+`|\\\"[^\\\"]+\\\"|'[^']+'|[a-z0-9_$]+)(?:\\.(?:`[^`]+`|\\\"[^\\\"]+\\\"|'[^']+'|[a-z0-9_$]+))?)");

  private CampaignContentConstraintViolationClassifier() {}

  static boolean matches(Throwable exception, String constraintName) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof ConstraintViolationException constraintViolation) {
        String actual = constraintViolation.getConstraintName();
        if (actual != null) {
          return matchesName(actual, constraintName);
        }
        return matchesMysqlMessage(
            constraintViolation.getSQLException().getMessage(), constraintName);
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean matchesName(String actual, String expected) {
    String normalizedActual = normalizeIdentifier(actual);
    String normalizedExpected = normalizeIdentifier(expected);
    int qualifierBoundary = normalizedActual.lastIndexOf('.');
    String unqualifiedActual =
        qualifierBoundary >= 0
            ? normalizedActual.substring(qualifierBoundary + 1)
            : normalizedActual;
    return unqualifiedActual.equals(normalizedExpected);
  }

  private static boolean matchesMysqlMessage(String message, String expected) {
    if (message == null) {
      return false;
    }
    Matcher matcher = MYSQL_CONSTRAINT_IDENTIFIER.matcher(message);
    while (matcher.find()) {
      if (matchesName(matcher.group(1), expected)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeIdentifier(String identifier) {
    return identifier
        .trim()
        .replace("`", "")
        .replace("\"", "")
        .replace("'", "")
        .toLowerCase(Locale.ROOT);
  }
}
