package app.bottlenote.statistics.service;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.global.timeseries.TimeSeriesRange;
import app.bottlenote.statistics.constant.MemberFunnelStage;
import app.bottlenote.statistics.domain.MemberCohort;
import app.bottlenote.statistics.domain.MemberCohortActivity;
import app.bottlenote.statistics.domain.MemberCohortRetentionRepository;
import app.bottlenote.statistics.domain.MemberFunnelCounts;
import app.bottlenote.statistics.domain.MemberFunnelRepository;
import app.bottlenote.statistics.dto.request.MemberCohortRetentionRequest;
import app.bottlenote.statistics.dto.request.MemberFunnelRequest;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse.CohortItem;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse.PointItem;
import app.bottlenote.statistics.dto.response.MemberFunnelResponse;
import app.bottlenote.statistics.dto.response.MemberFunnelResponse.StageItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberStatisticsService {

  static final Set<TimeSeriesGranularity> SUPPORTED =
      EnumSet.of(
          TimeSeriesGranularity.DAY, TimeSeriesGranularity.WEEK, TimeSeriesGranularity.MONTH);

  /** 퍼널의 방문 단계는 텔레메트리라서 방문자 통계와 같은 90일 상한을 따른다. */
  static final int FUNNEL_MAX_DAYS = 90;

  /** 코호트는 가입일 기준이고 활동은 롤업이라 보존 제한이 없다. 1년치 가입 코호트까지 허용한다. */
  static final int COHORT_MAX_DAYS = 366;

  static final int MAX_OFFSETS = 24;

  private static final ZoneId ZONE = ZoneId.of(TimeSeries.TIMEZONE);

  private final MemberFunnelRepository memberFunnelRepository;
  private final MemberCohortRetentionRepository memberCohortRetentionRepository;

  @Transactional(readOnly = true)
  public MemberFunnelResponse findFunnel(MemberFunnelRequest request) {
    TimeSeriesRange range =
        TimeSeriesRange.of(
            request.from(),
            request.to(),
            TimeSeriesGranularity.DAY,
            EnumSet.of(TimeSeriesGranularity.DAY),
            FUNNEL_MAX_DAYS,
            LocalDate.now(ZONE));
    MemberFunnelCounts counts =
        memberFunnelRepository.countFunnel(range.from(), range.toExclusive());

    long[] values = {counts.visitors(), counts.signedUp(), counts.agreed(), counts.activated()};
    MemberFunnelStage[] stages = MemberFunnelStage.values();
    List<StageItem> items = new ArrayList<>(stages.length);
    for (int index = 0; index < stages.length; index++) {
      Double conversionRate = index == 0 ? null : rate(values[index], values[index - 1]);
      double overallRate =
          index == 0 ? (values[0] == 0L ? 0.0 : 100.0) : rate(values[index], values[0]);
      items.add(
          new StageItem(
              stages[index], stages[index].getLabel(), values[index], conversionRate, overallRate));
    }
    return new MemberFunnelResponse(
        TimeSeries.format(range.from()), TimeSeries.format(range.to()), TimeSeries.TIMEZONE, items);
  }

  @Transactional(readOnly = true)
  public MemberCohortRetentionResponse findCohortRetention(MemberCohortRetentionRequest request) {
    LocalDateTime now = LocalDateTime.now(ZONE);
    TimeSeriesRange range =
        TimeSeriesRange.of(
            request.from(),
            request.to(),
            request.granularity(),
            SUPPORTED,
            COHORT_MAX_DAYS,
            now.toLocalDate());
    TimeSeriesGranularity granularity = range.granularity();
    int offsets = resolveOffsets(request.offsets(), granularity);

    Map<LocalDateTime, Long> membersByCohort = new HashMap<>();
    for (MemberCohort cohort :
        memberCohortRetentionRepository.countCohorts(
            range.from(), range.toExclusive(), granularity)) {
      membersByCohort.put(cohort.cohortAt(), cohort.members());
    }
    Map<LocalDateTime, Map<Integer, Long>> activeByCohort = new HashMap<>();
    for (MemberCohortActivity activity :
        memberCohortRetentionRepository.countActiveMembersByOffset(
            range.from(), range.toExclusive(), granularity, offsets)) {
      activeByCohort
          .computeIfAbsent(activity.cohortAt(), key -> new HashMap<>())
          .put(activity.offset(), activity.activeMembers());
    }

    // 요청 구간의 모든 코호트 버킷을 빠짐없이 내린다. 가입자가 없는 버킷은 0명이다.
    List<CohortItem> cohorts = new ArrayList<>();
    for (LocalDateTime cohortAt : range.buckets()) {
      long members = membersByCohort.getOrDefault(cohortAt, 0L);
      Map<Integer, Long> active = activeByCohort.getOrDefault(cohortAt, Map.of());
      List<PointItem> points = new ArrayList<>(offsets);
      LocalDateTime bucketStart = cohortAt;
      for (int offset = 0; offset < offsets; offset++) {
        LocalDateTime bucketEnd = granularity.next(bucketStart);
        points.add(
            point(
                offset,
                new BucketWindow(bucketStart, bucketEnd),
                members,
                active.get(offset),
                now));
        bucketStart = bucketEnd;
      }
      cohorts.add(new CohortItem(TimeSeries.format(cohortAt), members, points));
    }

    String coverageFrom =
        memberCohortRetentionRepository
            .findEarliestActivityDate()
            .map(LocalDate::toString)
            .orElse(null);
    return new MemberCohortRetentionResponse(
        granularity,
        TimeSeries.TIMEZONE,
        TimeSeries.format(range.from()),
        TimeSeries.format(range.to()),
        coverageFrom,
        offsets,
        cohorts);
  }

  private static PointItem point(
      int offset, BucketWindow window, long members, Long activeMembers, LocalDateTime now) {
    // 아직 시작되지 않은 버킷은 값이 없는 것이지 0이 아니다.
    if (!window.start().isBefore(now)) {
      return new PointItem(offset, null, null, true);
    }
    boolean partial = window.end().isAfter(now);
    long active = activeMembers == null ? 0L : activeMembers;
    Double retentionRate = members == 0L ? null : rate(active, members);
    return new PointItem(offset, active, retentionRate, partial);
  }

  /** offsets가 없으면 단위별 기본값, 있으면 1 이상 MAX_OFFSETS 이하로 자른다. */
  static int resolveOffsets(Integer requested, TimeSeriesGranularity granularity) {
    if (requested == null) {
      return switch (granularity) {
        case DAY -> 14;
        case WEEK -> 8;
        case MONTH -> 6;
        case HOUR -> throw new IllegalArgumentException("HOUR는 회원 코호트 통계에서 지원하지 않습니다.");
      };
    }
    return Math.max(1, Math.min(MAX_OFFSETS, requested));
  }

  private record BucketWindow(LocalDateTime start, LocalDateTime end) {}

  private static double rate(long numerator, long denominator) {
    if (denominator == 0L) {
      return 0.0;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
