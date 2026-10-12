package app.bottlenote.alcohols.constant;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public enum TastingTagGroupRole {
  CORE("대표 태그", "core", "이 맛을 결정짓는 태그. 추천 점수에 그대로 반영", ValueProvider.ADMIN, "1.0"),
  PERIPHERAL("보조 태그", "peripheral", "같이 나타나는 태그. 추천 점수에 0.4만 반영", ValueProvider.ADMIN, "0.4");

  private final String korName;
  private final String engName;
  private final String description;
  private final ValueProvider provider;
  private final BigDecimal weight;

  TastingTagGroupRole(
      String korName, String engName, String description, ValueProvider provider, String weight) {
    this.korName = korName;
    this.engName = engName;
    this.description = description;
    this.provider = provider;
    this.weight = new BigDecimal(weight);
  }

  public BigDecimal weight() {
    return weight;
  }
}
