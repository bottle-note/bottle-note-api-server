package app.bottlenote.statistics.facade;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;
import app.bottlenote.statistics.facade.payload.VisitorExclusionItem;
import java.time.LocalDateTime;

public interface VisitorStatisticsFacade {

  TimeSeries findActiveVisitors(VisitorStatisticsRequest request);

  TimeSeries findRetention(VisitorStatisticsRequest request);

  /** 기간 전체의 순 활성 회원 수. 버킷 합이 아니라 기간 안에서 중복을 제거한다. */
  long countActiveMembers(LocalDateTime from, LocalDateTime toExclusive);

  /** 방문자 통계와 같은 기준으로 다른 도메인 지표를 거르기 위한 제외 규칙. */
  VisitorExclusionItem getExclusion();
}
