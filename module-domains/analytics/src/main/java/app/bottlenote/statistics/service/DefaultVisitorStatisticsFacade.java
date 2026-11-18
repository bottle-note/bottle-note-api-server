package app.bottlenote.statistics.service;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;
import app.bottlenote.statistics.facade.VisitorStatisticsFacade;
import app.bottlenote.statistics.facade.payload.VisitorExclusionItem;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultVisitorStatisticsFacade implements VisitorStatisticsFacade {

  private final VisitorStatisticsService visitorStatisticsService;

  @Override
  public TimeSeries findActiveVisitors(VisitorStatisticsRequest request) {
    return visitorStatisticsService.findActiveVisitors(request);
  }

  @Override
  public TimeSeries findRetention(VisitorStatisticsRequest request) {
    return visitorStatisticsService.findRetention(request);
  }

  @Override
  public long countActiveMembers(LocalDateTime from, LocalDateTime toExclusive) {
    return visitorStatisticsService.countActiveMembers(from, toExclusive);
  }

  @Override
  public VisitorExclusionItem getExclusion() {
    return visitorStatisticsService.getExclusion();
  }
}
