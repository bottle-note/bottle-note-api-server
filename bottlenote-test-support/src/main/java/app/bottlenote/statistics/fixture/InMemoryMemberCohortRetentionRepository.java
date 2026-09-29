package app.bottlenote.statistics.fixture;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.domain.MemberCohort;
import app.bottlenote.statistics.domain.MemberCohortActivity;
import app.bottlenote.statistics.domain.MemberCohortRetentionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 회원 코호트 포트의 인메모리 구현. 테스트가 코호트와 offset별 활동 수를 미리 넣는다. */
public class InMemoryMemberCohortRetentionRepository implements MemberCohortRetentionRepository {

  private final List<MemberCohort> cohorts = new ArrayList<>();
  private final List<MemberCohortActivity> activities = new ArrayList<>();
  private LocalDate earliestActivityDate;
  private LocalDateTime lastFrom;
  private LocalDateTime lastToExclusive;
  private TimeSeriesGranularity lastGranularity;
  private Integer lastMaxOffsetExclusive;

  public void seedCohorts(MemberCohort... values) {
    cohorts.addAll(List.of(values));
  }

  public void seedActivities(MemberCohortActivity... values) {
    activities.addAll(List.of(values));
  }

  public void seedEarliestActivityDate(LocalDate date) {
    this.earliestActivityDate = date;
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

  public Integer lastMaxOffsetExclusive() {
    return lastMaxOffsetExclusive;
  }

  @Override
  public List<MemberCohort> countCohorts(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    remember(from, toExclusive, granularity);
    return cohorts.stream()
        .filter(cohort -> inRange(cohort.cohortAt(), from, toExclusive))
        .toList();
  }

  @Override
  public List<MemberCohortActivity> countActiveMembersByOffset(
      LocalDateTime from,
      LocalDateTime toExclusive,
      TimeSeriesGranularity granularity,
      int maxOffsetExclusive) {
    remember(from, toExclusive, granularity);
    this.lastMaxOffsetExclusive = maxOffsetExclusive;
    return activities.stream()
        .filter(activity -> inRange(activity.cohortAt(), from, toExclusive))
        .filter(activity -> activity.offset() < maxOffsetExclusive)
        .toList();
  }

  @Override
  public Optional<LocalDate> findEarliestActivityDate() {
    return Optional.ofNullable(earliestActivityDate);
  }

  private void remember(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity) {
    this.lastFrom = from;
    this.lastToExclusive = toExclusive;
    this.lastGranularity = granularity;
  }

  private static boolean inRange(
      LocalDateTime bucketAt, LocalDateTime from, LocalDateTime toExclusive) {
    return !bucketAt.isBefore(from) && bucketAt.isBefore(toExclusive);
  }
}
