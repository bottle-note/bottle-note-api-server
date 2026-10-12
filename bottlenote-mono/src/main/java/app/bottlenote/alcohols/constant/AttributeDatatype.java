package app.bottlenote.alcohols.constant;

import java.util.regex.Pattern;

/** 위스키 속성 값의 형식. 값은 문자열로 저장하고 이 형식으로 검증·변환한다. */
public enum AttributeDatatype {
  STRING(".+"),
  INT("-?\\d{1,18}"),
  DOUBLE("-?\\d{1,15}(\\.\\d{1,6})?"),
  BOOLEAN("true|false");

  private final Pattern pattern;

  AttributeDatatype(String regex) {
    this.pattern = Pattern.compile(regex, Pattern.DOTALL);
  }

  /** 값이 이 형식에 맞는지. 앞뒤 공백은 저장 전에 잘라 둔다는 전제다. */
  public boolean accepts(String value) {
    return value != null && !value.isBlank() && pattern.matcher(value).matches();
  }

  public boolean isNumeric() {
    return this == INT || this == DOUBLE;
  }
}
