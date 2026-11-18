package app.bottlenote.global.rating;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 별점 노출 정규화 정책.
 *
 * <p>집계 평점은 0.5 단위 양자화 없이 소수점 첫째 자리로만 맞춘다. 표시 규칙은 모든 노출 경로가 이 클래스를 공유한다.
 */
public final class RatingDisplay {

  /** 노출 정밀도 (소수점 첫째 자리) */
  public static final int SCALE = 1;

  /**
   * MySQL은 DOUBLE 평균의 .x5 경계를 half-even으로 처리해 half-up과 갈린다(예: 1.25 → 1.2). 경계에만 닿는 보정값을 더해 DB와
   * 애플리케이션의 반올림 결과를 일치시킨다.
   */
  private static final String HALF_UP_EPSILON = "0.000000001D";

  private RatingDisplay() {}

  /** QueryDSL 집계 표현식을 노출값으로 정규화한다. */
  public static NumberExpression<Double> normalize(NumberExpression<Double> average) {
    return Expressions.numberTemplate(
        Double.class, "round({0} + " + HALF_UP_EPSILON + ", " + SCALE + ")", average);
  }

  /** 애플리케이션에서 계산한 집계를 노출값으로 정규화한다. */
  public static Double normalize(Double average) {
    if (average == null) {
      return null;
    }
    return BigDecimal.valueOf(average).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
  }
}
