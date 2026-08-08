package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReport;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalView;
import app.bottlenote.accesscontrol.facade.IpSecuritySignalFacade;
import app.bottlenote.common.annotation.FacadeService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultIpSecuritySignalFacade implements IpSecuritySignalFacade {
  private final IpSecuritySignalService signalService;

  @Override
  public IpSecuritySignalView report(IpSecuritySignalReport report, Long reporterAdminUserId) {
    return signalService.report(report, reporterAdminUserId);
  }

  @Override
  public IpSecuritySignalView review(
      Long signalId, SignalVerdict verdict, String reviewNote, Long reviewerAdminUserId) {
    return signalService.review(signalId, verdict, reviewNote, reviewerAdminUserId);
  }

  @Override
  public Optional<IpSecuritySignalView> findById(Long signalId) {
    return signalService.getDetail(signalId);
  }

  @Override
  public List<IpSecuritySignalView> findByIp(String rawIp, int max) {
    return signalService.list(rawIp, max);
  }
}
