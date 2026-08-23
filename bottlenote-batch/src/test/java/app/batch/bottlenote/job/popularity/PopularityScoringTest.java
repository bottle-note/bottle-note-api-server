package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("batch")
@DisplayName("[batch] 인기도 점수 계산")
class PopularityScoringTest {

  private static final BigDecimal QUARTER = new BigDecimal("0.25");

  @Nested
  @DisplayName("정규화")
  class Normalize {

    @Test
    @DisplayName("기준값의 절반이면 0.5점이다")
    void halfOfReferenceScoresHalf() {
      assertThat(PopularityScoring.normalize(50L, 100L)).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("기준값에 도달하면 만점이다")
    void reachingReferenceScoresOne() {
      assertThat(PopularityScoring.normalize(100L, 100L)).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("기준값을 넘어도 1을 넘지 않는다")
    void exceedingReferenceIsCappedAtOne() {
      assertThat(PopularityScoring.normalize(10_000L, 100L)).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("값이 0이면 0점이다")
    void zeroValueScoresZero() {
      assertThat(PopularityScoring.normalize(0L, 100L)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("값이 음수여도 0점으로 눌린다")
    void negativeValueScoresZero() {
      // 픽 취소로 증감이 음수가 될 수 있으므로 방어한다
      assertThat(PopularityScoring.normalize(-5L, 100L)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("기준값이 0이면 나눌 수 없으므로 0점이다")
    void zeroReferenceScoresZeroInsteadOfFailing() {
      assertThat(PopularityScoring.normalize(50L, 0L)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("아주 큰 값도 오버플로 없이 만점으로 눌린다")
    void hugeValueDoesNotOverflow() {
      assertThat(PopularityScoring.normalize(Long.MAX_VALUE, 1L)).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("소수 넷째 자리까지 반올림한다")
    void keepsFourDecimalPlaces() {
      // 1/3 = 0.333333... → 0.3333
      assertThat(PopularityScoring.normalize(1L, 3L)).isEqualByComparingTo("0.3333");
      assertThat(PopularityScoring.normalize(1L, 3L).scale()).isEqualTo(PopularityScoring.SCALE);
    }
  }

  @Nested
  @DisplayName("가중 합산")
  class WeightedSum {

    @Test
    @DisplayName("네 축이 모두 만점이고 가중치 합이 1이면 정확히 1이다")
    void allPerfectScoresSumToExactlyOne() {
      BigDecimal one = PopularityScoring.normalize(100L, 100L);

      BigDecimal result =
          PopularityScoring.weightedSum(one, QUARTER, one, QUARTER, one, QUARTER, one, QUARTER);

      assertThat(result).isEqualByComparingTo("1.0000");
    }

    @Test
    @DisplayName("네 축이 모두 0이면 0이다")
    void allZeroScoresSumToZero() {
      BigDecimal zero = PopularityScoring.normalize(0L, 100L);

      BigDecimal result =
          PopularityScoring.weightedSum(zero, QUARTER, zero, QUARTER, zero, QUARTER, zero, QUARTER);

      assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("한 축만 만점이면 그 축의 가중치만큼만 오른다")
    void singlePerfectAxisContributesItsWeightOnly() {
      BigDecimal one = PopularityScoring.normalize(100L, 100L);
      BigDecimal zero = PopularityScoring.normalize(0L, 100L);

      BigDecimal result =
          PopularityScoring.weightedSum(one, QUARTER, zero, QUARTER, zero, QUARTER, zero, QUARTER);

      assertThat(result).isEqualByComparingTo("0.2500");
    }

    @Test
    @DisplayName("가중치를 기울이면 그 축의 영향이 커진다")
    void skewedWeightsShiftInfluence() {
      BigDecimal one = PopularityScoring.normalize(100L, 100L);
      BigDecimal zero = PopularityScoring.normalize(0L, 100L);

      BigDecimal result =
          PopularityScoring.weightedSum(
              one, new BigDecimal("0.70"),
              zero, new BigDecimal("0.10"),
              zero, new BigDecimal("0.10"),
              zero, new BigDecimal("0.10"));

      assertThat(result).isEqualByComparingTo("0.7000");
    }

    @Test
    @DisplayName("결과는 항상 소수 넷째 자리로 맞춰진다")
    void resultAlwaysHasFourDecimalPlaces() {
      BigDecimal third = PopularityScoring.normalize(1L, 3L);

      BigDecimal result =
          PopularityScoring.weightedSum(
              third, QUARTER, third, QUARTER, third, QUARTER, third, QUARTER);

      assertThat(result.scale()).isEqualTo(PopularityScoring.SCALE);
      assertThat(result).isEqualByComparingTo("0.3333");
    }
  }
}
