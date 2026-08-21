package app.bottlenote.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("RatingRangeValidator 단위 테스트")
class RatingRangeValidatorTest {

  @ParameterizedTest
  @CsvSource({"0.5,5.0", "3.0,4.0", "3.0,3.0"})
  @DisplayName("0.5 단위의 포함 범위는 유효하다")
  void 유효한_포함_범위(String from, String to) {
    assertThat(RatingRangeValidator.isValid(decimal(from), decimal(to))).isTrue();
  }

  @Test
  @DisplayName("한쪽 경계 또는 양쪽 경계를 생략할 수 있다")
  void 경계를_선택적으로_생략할_수_있다() {
    assertThat(RatingRangeValidator.isValid(decimal("3.0"), null)).isTrue();
    assertThat(RatingRangeValidator.isValid(null, decimal("4.0"))).isTrue();
    assertThat(RatingRangeValidator.isValid(null, null)).isTrue();
  }

  @ParameterizedTest
  @CsvSource({"0.0,5.0", "0.6,5.0", "0.5,5.1", "4.0,3.5"})
  @DisplayName("범위 밖·0.5 외 단위·역전 범위는 유효하지 않다")
  void 유효하지_않은_범위(String from, String to) {
    assertThat(RatingRangeValidator.isValid(decimal(from), decimal(to))).isFalse();
  }

  private BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }
}
