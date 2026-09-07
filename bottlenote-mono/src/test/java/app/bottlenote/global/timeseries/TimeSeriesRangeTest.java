package app.bottlenote.global.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("시계열 조회 구간")
class TimeSeriesRangeTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 7);
  private static final Set<TimeSeriesGranularity> ALL = EnumSet.allOf(TimeSeriesGranularity.class);

  @Test
  @DisplayName("날짜가 없으면 to는 today, from은 today-29일이다")
  void 기본_구간을_채울_수_있다() {
    TimeSeriesRange range =
        TimeSeriesRange.of(null, null, TimeSeriesGranularity.DAY, ALL, 30, TODAY);

    assertThat(range.from()).isEqualTo(LocalDateTime.of(2026, 8, 9, 0, 0));
    assertThat(range.to()).isEqualTo(LocalDateTime.of(2026, 9, 7, 0, 0));
    assertThat(range.buckets()).hasSize(30);
  }

  @Test
  @DisplayName("from이 to보다 뒤면 INVALID_RANGE다")
  void from이_to보다_뒤면_예외를_던질_수_있다() {
    assertThatThrownBy(
            () ->
                TimeSeriesRange.of(
                    LocalDate.of(2026, 9, 8),
                    LocalDate.of(2026, 9, 7),
                    TimeSeriesGranularity.DAY,
                    ALL,
                    30,
                    TODAY))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.INVALID_RANGE);
  }

  @Test
  @DisplayName("to가 today보다 뒤면 INVALID_RANGE다")
  void to가_today보다_뒤면_예외를_던질_수_있다() {
    assertThatThrownBy(
            () ->
                TimeSeriesRange.of(
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 8),
                    TimeSeriesGranularity.DAY,
                    ALL,
                    30,
                    TODAY))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.INVALID_RANGE);
  }

  @Test
  @DisplayName("포함 일수가 maxDays와 같으면 통과하고 초과하면 RANGE_TOO_LONG이다")
  void maxDays_경계를_검증할_수_있다() {
    TimeSeriesRange exact =
        TimeSeriesRange.of(
            LocalDate.of(2026, 8, 9),
            LocalDate.of(2026, 9, 7),
            TimeSeriesGranularity.DAY,
            ALL,
            30,
            TODAY);

    assertThat(exact.buckets()).hasSize(30);
    assertThatThrownBy(
            () ->
                TimeSeriesRange.of(
                    LocalDate.of(2026, 8, 8),
                    LocalDate.of(2026, 9, 7),
                    TimeSeriesGranularity.DAY,
                    ALL,
                    30,
                    TODAY))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.RANGE_TOO_LONG);
  }

  @Test
  @DisplayName("지원하지 않는 집계 단위면 UNSUPPORTED_GRANULARITY다")
  void 미지원_단위면_예외를_던질_수_있다() {
    assertThatThrownBy(
            () ->
                TimeSeriesRange.of(
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 7),
                    TimeSeriesGranularity.HOUR,
                    EnumSet.of(TimeSeriesGranularity.DAY, TimeSeriesGranularity.WEEK),
                    90,
                    TODAY))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY);
  }

  @Test
  @DisplayName("HOUR의 to는 그날 23:00 버킷이다")
  void HOUR_마지막_버킷을_23시로_둘_수_있다() {
    TimeSeriesRange range =
        TimeSeriesRange.of(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 1),
            TimeSeriesGranularity.HOUR,
            ALL,
            31,
            TODAY);

    assertThat(range.from()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
    assertThat(range.to()).isEqualTo(LocalDateTime.of(2026, 9, 1, 23, 0));
    assertThat(range.toExclusive()).isEqualTo(LocalDateTime.of(2026, 9, 2, 0, 0));
    assertThat(range.buckets()).hasSize(24);
  }
}
