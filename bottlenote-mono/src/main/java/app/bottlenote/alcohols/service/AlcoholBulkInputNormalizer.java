package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssue;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AlcoholBulkInputNormalizer {
  private static final String NUMBER = "[+-]?(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]+)?";
  private static final Pattern SCALAR =
      Pattern.compile("^(" + NUMBER + ")\\s*(%|ml|cl|l)?$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RANGE =
      Pattern.compile(
          "^(" + NUMBER + ")\\s*(%|ml|cl|l)?\\s*[-~–]\\s*(" + NUMBER + ")\\s*(%|ml|cl|l)?$",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SET_SUFFIX =
      Pattern.compile(
          "^(.+?)\\s*[x×*]\\s*([1-9][0-9]{0,5})(?:\\s*(?:병|bottles?))?$", Pattern.CASE_INSENSITIVE);
  private static final Pattern SET_PREFIX =
      Pattern.compile("^([1-9][0-9]{0,5})\\s*[x×*]\\s*(.+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern BATCH =
      Pattern.compile("^(?:batch|배치)\\s*[a-z0-9]+\\s*[:=]\\s*(.+)$", Pattern.CASE_INSENSITIVE);

  private AlcoholBulkInputNormalizer() {}

  static String clean(String value) {
    if (value == null) return null;
    String cleaned = value.replace('\u00a0', ' ').strip();
    return cleaned.isBlank() ? null : cleaned;
  }

  static String key(String value) {
    String cleaned = clean(value);
    return cleaned == null
        ? ""
        : Normalizer.normalize(cleaned, Normalizer.Form.NFKC)
            .replaceAll("[\\s\\p{Z}]+", " ")
            .toLowerCase(Locale.ROOT);
  }

  static String quantity(
      String raw,
      String field,
      List<AdminAlcoholBulkIssue> errors,
      List<AdminAlcoholBulkIssue> warnings) {
    String value = clean(raw);
    if (value == null) {
      errors.add(new AdminAlcoholBulkIssue("REQUIRED", field, "필수 입력값입니다."));
      return null;
    }
    if (value.length() > 255) {
      errors.add(new AdminAlcoholBulkIssue("TOO_LONG", field, "255자를 초과할 수 없습니다."));
      return null;
    }
    Matcher scalar = SCALAR.matcher(value);
    if (scalar.matches()) {
      BigDecimal number = scalarNumber(scalar.group(1), scalar.group(2), field);
      if (number != null) {
        String normalized =
            number.stripTrailingZeros().toPlainString() + (field.equals("abv") ? "%" : "ml");
        if (normalized.length() <= 255) return normalized;
        errors.add(new AdminAlcoholBulkIssue("TOO_LONG", field, "정규화한 값이 255자를 초과합니다."));
        return null;
      }
    } else if (isAnnotated(value, field) || isComposite(value, field)) {
      warnings.add(
          new AdminAlcoholBulkIssue("NON_SCALAR_VALUE", field, "범위·배치·세트·주석 표현을 원문으로 보존합니다."));
      return value;
    }
    errors.add(
        new AdminAlcoholBulkIssue(
            "INVALID_QUANTITY",
            field,
            field.equals("abv")
                ? "도수는 0~100의 숫자와 % 단위로 입력해 주세요."
                : "용량은 양수와 ml, cl, L 단위로 입력해 주세요."));
    return null;
  }

  private static boolean isAnnotated(String value, String field) {
    String scalar = value.replaceFirst("^(?:약|approx\\.?)\\s*", "");
    scalar = scalar.replaceAll("\\(\\s*(?:배치\\s*마다\\s*상이|제품\\s*마다\\s*상이|캐스크\\s*스트렝스)\\s*\\)", "");
    if (!scalar.equals(value) && validScalar(scalar, field)) return true;
    if (!field.equals("volume")) return false;
    int start = value.indexOf('(');
    if (start <= 0 || !value.endsWith(")")) return false;
    Matcher outside = SCALAR.matcher(value.substring(0, start).strip());
    Matcher inside = SCALAR.matcher(value.substring(start + 1, value.length() - 1).strip());
    if (!outside.matches() || !inside.matches()) return false;
    BigDecimal first = scalarNumber(outside.group(1), outside.group(2), field);
    BigDecimal second = scalarNumber(inside.group(1), inside.group(2), field);
    return first != null && second != null && first.compareTo(second) == 0;
  }

  private static boolean isComposite(String value, String field) {
    if (validRange(value, field)) return true;
    if (field.equals("volume")) {
      Matcher suffix = SET_SUFFIX.matcher(value);
      if (suffix.matches() && validScalar(suffix.group(1), field)) return true;
      Matcher prefix = SET_PREFIX.matcher(value);
      if (prefix.matches() && validScalar(prefix.group(2), field)) return true;
    }
    String[] parts = value.split("\\s*(?:/|;|\\+|,(?=\\s*(?:[Bb][Aa][Tt][Cc][Hh]|배치)))\\s*", -1);
    boolean batchFound = false;
    for (String part : parts) {
      Matcher batch = BATCH.matcher(part);
      if (batch.matches()) {
        batchFound = true;
        part = batch.group(1);
      }
      if (!validScalar(part, field) && !validRange(part, field)) return false;
    }
    return parts.length > 1 || batchFound;
  }

  private static boolean validScalar(String value, String field) {
    Matcher matcher = SCALAR.matcher(value);
    return matcher.matches() && scalarNumber(matcher.group(1), matcher.group(2), field) != null;
  }

  private static boolean validRange(String value, String field) {
    Matcher matcher = RANGE.matcher(value);
    if (!matcher.matches()) return false;
    String firstUnit = matcher.group(2) == null ? matcher.group(4) : matcher.group(2);
    String lastUnit = matcher.group(4) == null ? matcher.group(2) : matcher.group(4);
    BigDecimal first = scalarNumber(matcher.group(1), firstUnit, field);
    BigDecimal last = scalarNumber(matcher.group(3), lastUnit, field);
    return first != null && last != null && first.compareTo(last) <= 0;
  }

  private static BigDecimal scalarNumber(String value, String unit, String field) {
    BigDecimal number = new BigDecimal(value.replace(",", ""));
    String normalizedUnit = unit == null ? "" : unit.toLowerCase(Locale.ROOT);
    if (field.equals("abv")) {
      if ((!normalizedUnit.isEmpty() && !normalizedUnit.equals("%"))
          || number.signum() < 0
          || number.compareTo(BigDecimal.valueOf(100)) > 0) return null;
    } else {
      if (normalizedUnit.equals("%") || number.signum() <= 0) return null;
      if (normalizedUnit.equals("cl")) number = number.multiply(BigDecimal.TEN);
      if (normalizedUnit.equals("l")) number = number.multiply(BigDecimal.valueOf(1000));
    }
    return number;
  }
}
