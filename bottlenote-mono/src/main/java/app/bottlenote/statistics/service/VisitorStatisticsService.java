package app.bottlenote.statistics.service;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.global.timeseries.TimeSeriesAssembler;
import app.bottlenote.global.timeseries.TimeSeriesDescriptor;
import app.bottlenote.global.timeseries.TimeSeriesFill;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.global.timeseries.TimeSeriesRange;
import app.bottlenote.global.timeseries.TimeSeriesUnit;
import app.bottlenote.statistics.domain.ActiveVisitorBucket;
import app.bottlenote.statistics.domain.ReturningVisitorBucket;
import app.bottlenote.statistics.domain.VisitorStatisticsRepository;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitorStatisticsService {

  static final Set<TimeSeriesGranularity> SUPPORTED =
      EnumSet.of(
          TimeSeriesGranularity.DAY, TimeSeriesGranularity.WEEK, TimeSeriesGranularity.MONTH);
  static final int MAX_DAYS = 90;
  private static final ZoneId ZONE = ZoneId.of(TimeSeries.TIMEZONE);

  private final VisitorStatisticsRepository visitorStatisticsRepository;

  @Transactional(readOnly = true)
  public TimeSeries findActiveVisitors(VisitorStatisticsRequest request) {
    TimeSeriesRange range = rangeOf(request);
    List<ActiveVisitorBucket> buckets =
        visitorStatisticsRepository.countActiveVisitors(
            range.from(), range.toExclusive(), range.granularity());
    Map<LocalDateTime, Map<String, Number>> valuesByBucket = new LinkedHashMap<>();
    for (ActiveVisitorBucket bucket : buckets) {
      Map<String, Number> values = new LinkedHashMap<>();
      values.put("visitors", bucket.visitors());
      values.put("members", bucket.members());
      valuesByBucket.put(bucket.bucketAt(), values);
    }
    return TimeSeriesAssembler.assemble(
        range, activeSeries(range.granularity()), valuesByBucket, now());
  }

  @Transactional(readOnly = true)
  public TimeSeries findRetention(VisitorStatisticsRequest request) {
    TimeSeriesRange range = rangeOf(request);
    LocalDateTime queryFrom = range.granularity().previous(range.from());
    List<ReturningVisitorBucket> buckets =
        visitorStatisticsRepository.countReturningVisitors(
            queryFrom, range.toExclusive(), range.granularity());
    Map<LocalDateTime, Map<String, Number>> valuesByBucket = new LinkedHashMap<>();
    for (ReturningVisitorBucket bucket : buckets) {
      Map<String, Number> values = new LinkedHashMap<>();
      values.put("visitors", bucket.visitors());
      values.put("returningVisitors", bucket.returningVisitors());
      values.put("retentionRate", retentionRate(bucket.visitors(), bucket.returningVisitors()));
      valuesByBucket.put(bucket.bucketAt(), values);
    }
    return TimeSeriesAssembler.assemble(
        range, retentionSeries(range.granularity()), valuesByBucket, now());
  }

  private List<TimeSeriesDescriptor> activeSeries(TimeSeriesGranularity granularity) {
    String suffix = activitySuffix(granularity);
    return List.of(
        new TimeSeriesDescriptor(
            "visitors", "방문자 " + suffix, TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
        new TimeSeriesDescriptor(
            "members", "회원 " + suffix, TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO));
  }

  private List<TimeSeriesDescriptor> retentionSeries(TimeSeriesGranularity granularity) {
    String suffix = activitySuffix(granularity);
    return List.of(
        new TimeSeriesDescriptor(
            "visitors", "방문자 " + suffix, TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
        new TimeSeriesDescriptor(
            "returningVisitors", "재방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
        new TimeSeriesDescriptor(
            "retentionRate", "재방문율", TimeSeriesUnit.PERCENT, TimeSeriesFill.ZERO));
  }

  private String activitySuffix(TimeSeriesGranularity granularity) {
    return switch (granularity) {
      case DAY -> "DAU";
      case WEEK -> "WAU";
      case MONTH -> "MAU";
      case HOUR -> throw new IllegalArgumentException("HOUR는 방문자 통계에서 지원하지 않습니다.");
    };
  }

  private TimeSeriesRange rangeOf(VisitorStatisticsRequest request) {
    return TimeSeriesRange.of(
        request.from(),
        request.to(),
        request.granularity(),
        SUPPORTED,
        MAX_DAYS,
        LocalDate.now(ZONE));
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }

  private static Number retentionRate(long visitors, long returningVisitors) {
    if (visitors == 0L) {
      return 0.0;
    }
    return BigDecimal.valueOf(returningVisitors)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(visitors), 1, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
