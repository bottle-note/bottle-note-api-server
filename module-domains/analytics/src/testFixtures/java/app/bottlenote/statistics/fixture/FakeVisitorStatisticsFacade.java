package app.bottlenote.statistics.fixture;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;
import app.bottlenote.statistics.facade.VisitorStatisticsFacade;
import app.bottlenote.statistics.facade.payload.VisitorExclusionItem;
import java.time.LocalDateTime;
import java.util.List;

/** 다른 도메인 서비스 테스트용 방문자 통계 Facade. 시계열 조회는 이 Fake의 범위가 아니다. */
public class FakeVisitorStatisticsFacade implements VisitorStatisticsFacade {

  private long activeMembers;
  private VisitorExclusionItem exclusion = new VisitorExclusionItem(List.of(), List.of());
  private LocalDateTime lastFrom;
  private LocalDateTime lastToExclusive;

  public void setActiveMembers(long activeMembers) {
    this.activeMembers = activeMembers;
  }

  public void setExclusion(VisitorExclusionItem exclusion) {
    this.exclusion = exclusion;
  }

  public LocalDateTime lastFrom() {
    return lastFrom;
  }

  public LocalDateTime lastToExclusive() {
    return lastToExclusive;
  }

  @Override
  public TimeSeries findActiveVisitors(VisitorStatisticsRequest request) {
    throw new UnsupportedOperationException("시계열 조회는 VisitorStatisticsServiceTest에서 검증한다");
  }

  @Override
  public TimeSeries findRetention(VisitorStatisticsRequest request) {
    throw new UnsupportedOperationException("시계열 조회는 VisitorStatisticsServiceTest에서 검증한다");
  }

  @Override
  public long countActiveMembers(LocalDateTime from, LocalDateTime toExclusive) {
    this.lastFrom = from;
    this.lastToExclusive = toExclusive;
    return activeMembers;
  }

  @Override
  public VisitorExclusionItem getExclusion() {
    return exclusion;
  }
}
