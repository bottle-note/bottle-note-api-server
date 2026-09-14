package app.bottlenote.statistics.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.time.LocalDateTime;
import java.util.List;

@DomainRepository
public interface VisitorStatisticsRepository {

  List<ActiveVisitorBucket> countActiveVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity);

  List<ReturningVisitorBucket> countReturningVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity);
}
