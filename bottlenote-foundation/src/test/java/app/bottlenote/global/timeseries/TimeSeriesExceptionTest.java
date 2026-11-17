package app.bottlenote.global.timeseries;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("unit")
@DisplayName("시계열 예외")
class TimeSeriesExceptionTest {

  @Test
  @DisplayName("코드별 HTTP 상태는 400이다")
  void 코드별_HttpStatus가_400일_수_있다() {
    assertThat(TimeSeriesExceptionCode.INVALID_RANGE.getHttpStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(TimeSeriesExceptionCode.RANGE_TOO_LONG.getHttpStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY.getHttpStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(new TimeSeriesException(TimeSeriesExceptionCode.INVALID_RANGE).getExceptionCode())
        .isEqualTo(TimeSeriesExceptionCode.INVALID_RANGE);
  }
}
