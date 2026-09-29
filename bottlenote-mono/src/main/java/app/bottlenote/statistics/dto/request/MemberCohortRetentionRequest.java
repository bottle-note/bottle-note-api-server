package app.bottlenote.statistics.dto.request;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.time.LocalDate;

/**
 * 가입 코호트 리텐션 조회 조건. from·to는 가입일 구간이다.
 *
 * @param offsets 코호트마다 내릴 버킷 수. 생략하면 단위별 기본값이다.
 */
public record MemberCohortRetentionRequest(
    LocalDate from, LocalDate to, TimeSeriesGranularity granularity, Integer offsets) {

  public MemberCohortRetentionRequest {
    granularity = granularity == null ? TimeSeriesGranularity.WEEK : granularity;
  }
}
