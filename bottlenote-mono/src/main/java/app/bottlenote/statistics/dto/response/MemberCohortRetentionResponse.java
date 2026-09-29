package app.bottlenote.statistics.dto.response;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.util.List;

/**
 * 가입 코호트 리텐션 행렬. 행은 가입 버킷, 열은 가입 버킷으로부터의 offset이다.
 *
 * @param coverageFrom 회원 일별 활동 롤업이 시작된 날(ISO 날짜). 롤업이 비어 있으면 null. 이보다 앞선 코호트의 활동은 관측되지 않은 것이다
 * @param offsets 코호트마다 내린 열 수
 */
public record MemberCohortRetentionResponse(
    TimeSeriesGranularity granularity,
    String timezone,
    String from,
    String to,
    String coverageFrom,
    int offsets,
    List<CohortItem> cohorts) {

  public MemberCohortRetentionResponse {
    cohorts = cohorts == null ? List.of() : List.copyOf(cohorts);
  }

  /**
   * @param members 코호트 가입자 수
   * @param points offset 0부터 offsets-1까지 순서대로
   */
  public record CohortItem(String cohortAt, long members, List<PointItem> points) {
    public CohortItem {
      points = points == null ? List.of() : List.copyOf(points);
    }
  }

  /**
   * @param activeMembers 해당 offset 버킷에 활동한 회원 수. 버킷이 아직 시작되지 않았으면 null
   * @param retentionRate activeMembers / members (%). members가 0이거나 버킷이 시작되지 않았으면 null
   * @param partial 버킷이 요청 시점에 아직 닫히지 않았으면 true
   */
  public record PointItem(int offset, Long activeMembers, Double retentionRate, boolean partial) {}
}
