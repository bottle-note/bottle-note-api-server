package app.bottlenote.alcohols.constant;

import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public enum AttributeDatatype implements CodeLabel {
  STRING("텍스트", "string", "자유 문자열", ValueProvider.ANY, ".+"),
  INT("정수", "int", "소수점 없는 정수. 가격처럼 원 단위 값", ValueProvider.ANY, "-?\\d{1,18}"),
  DOUBLE(
      "소수",
      "double",
      "소수점을 포함할 수 있는 숫자. 점수처럼 비율 값",
      ValueProvider.ANY,
      "-?\\d{1,15}(\\.\\d{1,6})?"),
  BOOLEAN("예/아니오", "boolean", "true 또는 false", ValueProvider.ANY, "true|false");

  private final String korName;
  private final String engName;
  private final String description;
  private final ValueProvider provider;
  private final Pattern pattern;

  AttributeDatatype(
      String korName, String engName, String description, ValueProvider provider, String regex) {
    this.korName = korName;
    this.engName = engName;
    this.description = description;
    this.provider = provider;
    this.pattern = Pattern.compile(regex, Pattern.DOTALL);
  }

  public boolean accepts(String value) {
    return value != null && !value.isBlank() && pattern.matcher(value).matches();
  }

  public boolean isNumeric() {
    return this == INT || this == DOUBLE;
  }
}
