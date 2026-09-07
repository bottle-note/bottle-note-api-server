package app.bottlenote.global.timeseries;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TimeSeriesAssembler {

  private TimeSeriesAssembler() {}

  public static TimeSeries assemble(
      TimeSeriesRange range,
      List<TimeSeriesDescriptor> series,
      Map<LocalDateTime, Map<String, Number>> valuesByBucket,
      LocalDateTime now) {
    Objects.requireNonNull(range, "range는 null일 수 없습니다.");
    Objects.requireNonNull(series, "series는 null일 수 없습니다.");
    Objects.requireNonNull(valuesByBucket, "valuesByBucket는 null일 수 없습니다.");
    Objects.requireNonNull(now, "now는 null일 수 없습니다.");

    List<TimeSeriesDescriptor> descriptors = List.copyOf(series);
    List<TimeSeriesPoint> points = new ArrayList<>();
    Map<String, Number> previousValues = null;

    for (LocalDateTime bucketStart : range.buckets()) {
      Map<String, Number> source = valuesByBucket.get(bucketStart);
      if (source == null) {
        source = Map.of();
      }
      Map<String, Number> values = new LinkedHashMap<>();
      for (TimeSeriesDescriptor descriptor : descriptors) {
        String key = descriptor.key();
        values.put(key, resolveValue(descriptor, source, previousValues));
      }
      boolean partial = range.granularity().next(bucketStart).isAfter(now);
      points.add(new TimeSeriesPoint(TimeSeries.format(bucketStart), partial, values));
      previousValues = values;
    }

    return new TimeSeries(
        range.granularity(),
        TimeSeries.TIMEZONE,
        TimeSeries.format(range.from()),
        TimeSeries.format(range.to()),
        descriptors,
        points);
  }

  private static Number resolveValue(
      TimeSeriesDescriptor descriptor,
      Map<String, Number> source,
      Map<String, Number> previousValues) {
    String key = descriptor.key();
    if (source.containsKey(key)) {
      return source.get(key);
    }
    return switch (descriptor.fill()) {
      case ZERO -> 0L;
      case NULL -> null;
      case PREVIOUS -> previousValues == null ? null : previousValues.get(key);
    };
  }
}
