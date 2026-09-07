package app.bottlenote.global.timeseries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TimeSeriesRange(
    LocalDateTime from, LocalDateTime to, TimeSeriesGranularity granularity) {

  public TimeSeriesRange {
    Objects.requireNonNull(from, "from은 null일 수 없습니다.");
    Objects.requireNonNull(to, "to는 null일 수 없습니다.");
    Objects.requireNonNull(granularity, "granularity는 null일 수 없습니다.");
  }

  public static TimeSeriesRange of(
      LocalDate from,
      LocalDate to,
      TimeSeriesGranularity granularity,
      Set<TimeSeriesGranularity> supported,
      int maxDays,
      LocalDate today) {
    Objects.requireNonNull(granularity, "granularity는 null일 수 없습니다.");
    Objects.requireNonNull(supported, "supported는 null일 수 없습니다.");
    Objects.requireNonNull(today, "today는 null일 수 없습니다.");

    LocalDate resolvedTo = to == null ? today : to;
    LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;

    if (!supported.contains(granularity)) {
      throw new TimeSeriesException(TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY);
    }
    if (resolvedFrom.isAfter(resolvedTo)) {
      throw new TimeSeriesException(TimeSeriesExceptionCode.INVALID_RANGE);
    }
    // 검증은 truncate 전 원본 날짜 기준이다.
    if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > maxDays) {
      throw new TimeSeriesException(TimeSeriesExceptionCode.RANGE_TOO_LONG);
    }

    LocalDateTime fromBucket = granularity.truncate(resolvedFrom.atStartOfDay());
    LocalDateTime toBucket =
        granularity == TimeSeriesGranularity.HOUR
            ? granularity.truncate(resolvedTo.atTime(23, 0))
            : granularity.truncate(resolvedTo.atStartOfDay());
    return new TimeSeriesRange(fromBucket, toBucket, granularity);
  }

  public LocalDateTime toExclusive() {
    return granularity.next(to);
  }

  public List<LocalDateTime> buckets() {
    return granularity.bucketsBetween(from, toExclusive());
  }
}
