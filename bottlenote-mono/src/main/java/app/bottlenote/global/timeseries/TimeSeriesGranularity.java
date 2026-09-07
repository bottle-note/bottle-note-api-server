package app.bottlenote.global.timeseries;

import app.bottlenote.global.annotation.ExcludeRule;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 시계열 버킷 단위. 주는 월요일 시작이다. */
@ExcludeRule
public enum TimeSeriesGranularity {
  HOUR,
  DAY,
  WEEK,
  MONTH;

  public LocalDateTime truncate(LocalDateTime dateTime) {
    Objects.requireNonNull(dateTime, "dateTime은 null일 수 없습니다.");
    return switch (this) {
      case HOUR -> dateTime.truncatedTo(ChronoUnit.HOURS);
      case DAY -> dateTime.toLocalDate().atStartOfDay();
      case WEEK ->
          dateTime
              .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
              .toLocalDate()
              .atStartOfDay();
      case MONTH -> dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay();
    };
  }

  public LocalDateTime next(LocalDateTime bucketStart) {
    requireBucketStart(bucketStart);
    return switch (this) {
      case HOUR -> bucketStart.plusHours(1);
      case DAY -> bucketStart.plusDays(1);
      case WEEK -> bucketStart.plusWeeks(1);
      case MONTH -> bucketStart.plusMonths(1);
    };
  }

  public LocalDateTime previous(LocalDateTime bucketStart) {
    requireBucketStart(bucketStart);
    return switch (this) {
      case HOUR -> bucketStart.minusHours(1);
      case DAY -> bucketStart.minusDays(1);
      case WEEK -> bucketStart.minusWeeks(1);
      case MONTH -> bucketStart.minusMonths(1);
    };
  }

  public List<LocalDateTime> bucketsBetween(
      LocalDateTime fromInclusive, LocalDateTime toExclusive) {
    Objects.requireNonNull(fromInclusive, "fromInclusive는 null일 수 없습니다.");
    Objects.requireNonNull(toExclusive, "toExclusive는 null일 수 없습니다.");
    requireBucketStart(fromInclusive);
    List<LocalDateTime> buckets = new ArrayList<>();
    LocalDateTime current = fromInclusive;
    while (current.isBefore(toExclusive)) {
      buckets.add(current);
      current = next(current);
    }
    return List.copyOf(buckets);
  }

  private void requireBucketStart(LocalDateTime bucketStart) {
    Objects.requireNonNull(bucketStart, "bucketStart는 null일 수 없습니다.");
    if (!truncate(bucketStart).equals(bucketStart)) {
      throw new IllegalArgumentException("버킷 시작 시각이어야 합니다.");
    }
  }
}
