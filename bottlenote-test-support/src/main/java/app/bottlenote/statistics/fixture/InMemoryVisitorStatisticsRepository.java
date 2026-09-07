package app.bottlenote.statistics.fixture;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.domain.ActiveVisitorBucket;
import app.bottlenote.statistics.domain.ReturningVisitorBucket;
import app.bottlenote.statistics.domain.VisitorStatisticsRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 방문자 통계 포트의 인메모리 구현. 테스트가 버킷 결과를 미리 넣는다. */
public class InMemoryVisitorStatisticsRepository implements VisitorStatisticsRepository {

  private final List<ActiveVisitorBucket> activeBuckets = new ArrayList<>();
  private final List<ReturningVisitorBucket> returningBuckets = new ArrayList<>();
  private LocalDateTime lastFrom;
  private LocalDateTime lastToExclusive;
  private TimeSeriesGranularity lastGranularity;

  public void seedActive(ActiveVisitorBucket... buckets) {
    activeBuckets.addAll(List.of(buckets));
  }

  public void seedReturning(ReturningVisitorBucket... buckets) {
    returningBuckets.addAll(List.of(buckets));
  }

  public LocalDateTime lastFrom() {
    return lastFrom;
  }

  public LocalDateTime lastToExclusive() {
    return lastToExclusive;
  }

  public TimeSeriesGranularity lastGranularity() {
    return lastGranularity;
  }

  @Override
  public List<ActiveVisitorBucket> countActiveVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    remember(from, toExclusive, granularity);
    return activeBuckets.stream()
        .filter(bucket -> inRange(bucket.bucketAt(), from, toExclusive))
        .toList();
  }

  @Override
  public List<ReturningVisitorBucket> countReturningVisitors(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    remember(from, toExclusive, granularity);
    return returningBuckets.stream()
        .filter(bucket -> inRange(bucket.bucketAt(), from, toExclusive))
        .toList();
  }

  private void remember(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    this.lastFrom = from;
    this.lastToExclusive = toExclusive;
    this.lastGranularity = granularity;
  }

  private static boolean inRange(LocalDateTime bucketAt, LocalDateTime from, LocalDateTime toExclusive) {
    return !bucketAt.isBefore(from) && bucketAt.isBefore(toExclusive);
  }
}
