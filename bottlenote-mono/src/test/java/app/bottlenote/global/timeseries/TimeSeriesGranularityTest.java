package app.bottlenote.global.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("시계열 집계 단위 버킷 경계")
class TimeSeriesGranularityTest {

  @Test
  @DisplayName("시간 단위는 정시로 자르고 한 시간씩 이동한다")
  void HOUR_경계를_계산할_수_있다() {
    LocalDateTime raw = LocalDateTime.of(2026, 9, 7, 14, 37, 12);
    LocalDateTime start = TimeSeriesGranularity.HOUR.truncate(raw);

    assertThat(start).isEqualTo(LocalDateTime.of(2026, 9, 7, 14, 0));
    assertThat(TimeSeriesGranularity.HOUR.next(start))
        .isEqualTo(LocalDateTime.of(2026, 9, 7, 15, 0));
    assertThat(TimeSeriesGranularity.HOUR.previous(start))
        .isEqualTo(LocalDateTime.of(2026, 9, 7, 13, 0));
  }

  @Test
  @DisplayName("일 단위는 그날 00:00으로 자르고 하루씩 이동한다")
  void DAY_경계를_계산할_수_있다() {
    LocalDateTime raw = LocalDateTime.of(2026, 9, 7, 14, 37);
    LocalDateTime start = TimeSeriesGranularity.DAY.truncate(raw);

    assertThat(start).isEqualTo(LocalDateTime.of(2026, 9, 7, 0, 0));
    assertThat(TimeSeriesGranularity.DAY.next(start)).isEqualTo(LocalDateTime.of(2026, 9, 8, 0, 0));
    assertThat(TimeSeriesGranularity.DAY.previous(start))
        .isEqualTo(LocalDateTime.of(2026, 9, 6, 0, 0));
  }

  @Test
  @DisplayName("일요일은 직전 월요일 00:00으로 자르고 월요일은 그대로 둔다")
  void WEEK_주_경계를_계산할_수_있다() {
    LocalDateTime sunday = LocalDateTime.of(2026, 9, 6, 23, 59);
    LocalDateTime monday = LocalDateTime.of(2026, 8, 31, 10, 15);
    LocalDateTime weekStart = TimeSeriesGranularity.WEEK.truncate(sunday);

    assertThat(weekStart).isEqualTo(LocalDateTime.of(2026, 8, 31, 0, 0));
    assertThat(TimeSeriesGranularity.WEEK.truncate(monday)).isEqualTo(weekStart);
    assertThat(TimeSeriesGranularity.WEEK.next(weekStart))
        .isEqualTo(LocalDateTime.of(2026, 9, 7, 0, 0));
    assertThat(TimeSeriesGranularity.WEEK.previous(weekStart))
        .isEqualTo(LocalDateTime.of(2026, 8, 24, 0, 0));
  }

  @Test
  @DisplayName("월 단위는 1일 00:00으로 자르고 월말·윤년 날짜도 같은 달 시작으로 모은다")
  void MONTH_월말과_윤년을_계산할_수_있다() {
    LocalDateTime monthEnd = LocalDateTime.of(2026, 1, 31, 23, 59);
    LocalDateTime leapDay = LocalDateTime.of(2028, 2, 29, 15, 0);
    LocalDateTime january = TimeSeriesGranularity.MONTH.truncate(monthEnd);
    LocalDateTime february = TimeSeriesGranularity.MONTH.truncate(leapDay);

    assertThat(january).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    assertThat(TimeSeriesGranularity.MONTH.next(january))
        .isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
    assertThat(TimeSeriesGranularity.MONTH.previous(january))
        .isEqualTo(LocalDateTime.of(2025, 12, 1, 0, 0));
    assertThat(february).isEqualTo(LocalDateTime.of(2028, 2, 1, 0, 0));
    assertThat(TimeSeriesGranularity.MONTH.next(february))
        .isEqualTo(LocalDateTime.of(2028, 3, 1, 0, 0));
  }

  @Test
  @DisplayName("구간 버킷은 시작 포함·끝 제외 오름차순이다")
  void bucketsBetween_개수와_순서를_반환할_수_있다() {
    List<LocalDateTime> hours =
        TimeSeriesGranularity.HOUR.bucketsBetween(
            LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 1, 13, 0));
    List<LocalDateTime> days =
        TimeSeriesGranularity.DAY.bucketsBetween(
            LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 4, 0, 0));
    List<LocalDateTime> weeks =
        TimeSeriesGranularity.WEEK.bucketsBetween(
            LocalDateTime.of(2026, 8, 31, 0, 0), LocalDateTime.of(2026, 9, 14, 0, 0));
    List<LocalDateTime> months =
        TimeSeriesGranularity.MONTH.bucketsBetween(
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 4, 1, 0, 0));

    assertThat(hours)
        .containsExactly(
            LocalDateTime.of(2026, 9, 1, 10, 0),
            LocalDateTime.of(2026, 9, 1, 11, 0),
            LocalDateTime.of(2026, 9, 1, 12, 0));
    assertThat(days)
        .containsExactly(
            LocalDateTime.of(2026, 9, 1, 0, 0),
            LocalDateTime.of(2026, 9, 2, 0, 0),
            LocalDateTime.of(2026, 9, 3, 0, 0));
    assertThat(weeks)
        .containsExactly(LocalDateTime.of(2026, 8, 31, 0, 0), LocalDateTime.of(2026, 9, 7, 0, 0));
    assertThat(months)
        .containsExactly(
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 2, 1, 0, 0),
            LocalDateTime.of(2026, 3, 1, 0, 0));
  }

  @Test
  @DisplayName("버킷 시작이 아니면 next와 previous, bucketsBetween이 예외를 던진다")
  void 버킷_시작이_아니면_예외를_던질_수_있다() {
    LocalDateTime notStart = LocalDateTime.of(2026, 9, 7, 14, 1);

    assertThatThrownBy(() -> TimeSeriesGranularity.HOUR.next(notStart))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> TimeSeriesGranularity.DAY.previous(notStart))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                TimeSeriesGranularity.WEEK.bucketsBetween(
                    notStart, LocalDateTime.of(2026, 9, 14, 0, 0)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
