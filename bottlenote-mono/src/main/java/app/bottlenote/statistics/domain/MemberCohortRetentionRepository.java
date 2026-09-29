package app.bottlenote.statistics.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface MemberCohortRetentionRepository {

  /** 가입일이 구간 안인 회원을 버킷별로 센다. */
  List<MemberCohort> countCohorts(
      LocalDateTime from, LocalDateTime toExclusive, TimeSeriesGranularity granularity);

  /** 각 코호트가 offset번째 버킷에 활동한 회원 수. offset은 0부터 maxOffsetExclusive 미만이다. */
  List<MemberCohortActivity> countActiveMembersByOffset(
      LocalDateTime from,
      LocalDateTime toExclusive,
      TimeSeriesGranularity granularity,
      int maxOffsetExclusive);

  /** 롤업이 쌓이기 시작한 날. 이보다 앞선 코호트의 활동은 관측되지 않은 것이지 0이 아니다. */
  Optional<LocalDate> findEarliestActivityDate();
}
