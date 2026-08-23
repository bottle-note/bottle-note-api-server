package app.batch.bottlenote.job.popularity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 인기도 점수 계산.
 *
 * <p>적재 경로에서 떼어내 순수 함수로 둔다. 산식은 확정되지 않았고 앞으로 여러 번 바뀔 텐데, 바뀔 때마다 DB 없이 검증할 수 있어야 한다.
 */
public final class PopularityScoring {

  public static final int SCALE = 4;

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
  private static final BigDecimal ONE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);

  private PopularityScoring() {}

  /**
   * 축 값을 0~1로 누른다. 기준값에 도달하면 만점이고 그 위로는 오르지 않는다.
   *
   * <p>값이나 기준이 유효하지 않으면 0점이다 — 기준이 0이면 나눌 수 없고, 음수 값은 정의되지 않는다.
   */
  public static BigDecimal normalize(long value, long reference) {
    if (value <= 0L || reference <= 0L) {
      return ZERO;
    }
    BigDecimal score =
        BigDecimal.valueOf(value)
            .divide(BigDecimal.valueOf(reference), SCALE, RoundingMode.HALF_UP);
    return score.compareTo(ONE) > 0 ? ONE : score;
  }

  /** 축별 점수를 가중 합산한다. 가중치 합이 1이면 결과는 0~1에 머문다. */
  public static BigDecimal weightedSum(
      BigDecimal interestScore,
      BigDecimal interestWeight,
      BigDecimal ratingScore,
      BigDecimal ratingWeight,
      BigDecimal pickScore,
      BigDecimal pickWeight,
      BigDecimal engagementScore,
      BigDecimal engagementWeight) {
    return interestScore
        .multiply(interestWeight)
        .add(ratingScore.multiply(ratingWeight))
        .add(pickScore.multiply(pickWeight))
        .add(engagementScore.multiply(engagementWeight))
        .setScale(SCALE, RoundingMode.HALF_UP);
  }
}
