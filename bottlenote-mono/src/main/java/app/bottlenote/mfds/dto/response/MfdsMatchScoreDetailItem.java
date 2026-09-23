package app.bottlenote.mfds.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** v2 점수와 비교 근거이며 UNKNOWN은 불일치가 아닌 정보 부족을 뜻한다. */
public record MfdsMatchScoreDetailItem(
    BigDecimal nameScore,
    BigDecimal abvScore,
    BigDecimal ageScore,
    BigDecimal categoryScore,
    BigDecimal regionScore,
    BigDecimal totalScore,
    BigDecimal brandScore,
    boolean reviewRequired,
    List<AttributeComparison> comparisons) {
  /** 속성별 원문 추출 값과 MATCH, MISMATCH, UNKNOWN 판정. */
  public record AttributeComparison(
      String attribute, String sourceValue, String targetValue, String status) {}
}
