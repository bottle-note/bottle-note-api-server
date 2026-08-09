package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReportRequest;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalResponse;
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
  public IpSecuritySignalResponse report(
      IpSecuritySignalReportRequest report, Long reporterAdminUserId) {
    return signalService.report(report, reporterAdminUserId);
  }

  @Override
  public IpSecuritySignalResponse review(
      Long signalId, SignalVerdict verdict, String reviewNote, Long reviewerAdminUserId) {
    return signalService.review(signalId, verdict, reviewNote, reviewerAdminUserId);
  }

  @Override
  public Optional<IpSecuritySignalResponse> findById(Long signalId) {
    return signalService.getDetail(signalId);
  }

  @Override
  public List<IpSecuritySignalResponse> findByIp(String rawIp, int max) {
    return signalService.list(rawIp, max);
  }
}
