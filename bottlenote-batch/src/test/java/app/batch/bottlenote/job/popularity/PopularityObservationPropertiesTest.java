package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 설정 검증.
 *
 * <p>이전 인기도 배치는 검증 메서드를 만들어 두고 호출하지 않아, 가중치 합이 맞지 않아도 조용히 통과했다. 같은 실패를 반복하지 않으려면 검증이 실제로 기동을
 * 막는지 확인해야 한다.
 */
@Tag("batch")
@DisplayName("[batch] 인기도 관측 설정")
class PopularityObservationPropertiesTest {

  @Test
  @DisplayName("기본값은 그대로 유효하다")
  void defaultsAreValid() {
    PopularityObservationProperties properties = new PopularityObservationProperties();

    assertThat(properties.validate()).isEmpty();
    assertThat(properties.isValid()).isTrue();
  }

  @Test
  @DisplayName("기본 가중치는 네 축 균등이고 합이 정확히 1이다")
  void defaultWeightsSumToOne() {
    var weights = new PopularityObservationProperties().getWeights();

    assertThat(weights.getInterest()).isEqualByComparingTo("0.25");
    assertThat(
            weights
                .getInterest()
                .add(weights.getRating())
                .add(weights.getPick())
                .add(weights.getEngagement()))
        .isEqualByComparingTo("1.0");
  }

  @Test
  @DisplayName("가중치 합이 1이 아니면 기동에 실패한다")
  void startupFailsWhenWeightsDoNotSumToOne() {
    PopularityObservationProperties properties = new PopularityObservationProperties();
    properties.getWeights().setInterest(new BigDecimal("0.50"));

    assertThat(properties.validate()).isNotEmpty();
    assertThatThrownBy(properties::verifyOnStartup)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("가중치 합");
  }

  @Test
  @DisplayName("정규화 기준은 시간 주 월별로 독립 관리한다")
  void referencesAreIndependentByGranularity() {
    PopularityObservationProperties properties = new PopularityObservationProperties();
    properties.getReference().getWeek().setInterest(700L);

    assertThat(properties.referenceFor(BucketGranularity.HOUR).getInterest()).isEqualTo(50L);
    assertThat(properties.referenceFor(BucketGranularity.WEEK).getInterest()).isEqualTo(700L);
    assertThat(properties.referenceFor(BucketGranularity.MONTH).getInterest()).isEqualTo(50L);
  }

  @Test
  @DisplayName("정규화 기준이 0 이하면 기동에 실패한다")
  void startupFailsWhenReferenceIsNotPositive() {
    PopularityObservationProperties properties = new PopularityObservationProperties();
    properties.getReference().getMonth().setPick(0L);

    assertThatThrownBy(properties::verifyOnStartup)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MONTH 선호도 정규화 기준");
  }

  @Test
  @DisplayName("가중치가 0~1 범위를 벗어나면 오류로 잡는다")
  void rejectsWeightOutsideUnitRange() {
    PopularityObservationProperties properties = new PopularityObservationProperties();
    properties.getWeights().setInterest(new BigDecimal("-0.25"));
    properties.getWeights().setRating(new BigDecimal("0.50"));

    assertThat(properties.validate()).anyMatch(error -> error.contains("관심도 가중치"));
  }
}
