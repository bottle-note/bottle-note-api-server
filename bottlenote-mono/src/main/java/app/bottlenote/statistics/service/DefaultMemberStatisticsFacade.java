package app.bottlenote.statistics.service;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.statistics.dto.request.MemberCohortRetentionRequest;
import app.bottlenote.statistics.dto.request.MemberFunnelRequest;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse;
import app.bottlenote.statistics.dto.response.MemberFunnelResponse;
import app.bottlenote.statistics.facade.MemberStatisticsFacade;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultMemberStatisticsFacade implements MemberStatisticsFacade {

  private final MemberStatisticsService memberStatisticsService;

  @Override
  public MemberFunnelResponse findFunnel(MemberFunnelRequest request) {
    return memberStatisticsService.findFunnel(request);
  }

  @Override
  public MemberCohortRetentionResponse findCohortRetention(MemberCohortRetentionRequest request) {
    return memberStatisticsService.findCohortRetention(request);
  }
}
