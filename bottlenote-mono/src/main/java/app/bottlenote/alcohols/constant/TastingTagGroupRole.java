package app.bottlenote.alcohols.constant;

import java.math.BigDecimal;

/** 맛 그룹 안에서 태그의 역할. 추천 점수 계산 시 가중치가 다르다. */
public enum TastingTagGroupRole {
  /** 이 맛을 결정짓는 대표 태그 */
  CORE(new BigDecimal("1.0")),
  /** 같이 나타나는 보조 태그 */
  PERIPHERAL(new BigDecimal("0.4"));

  private final BigDecimal weight;

  TastingTagGroupRole(BigDecimal weight) {
    this.weight = weight;
  }

  public BigDecimal weight() {
    return weight;
  }
}
