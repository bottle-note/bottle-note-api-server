package app.bottlenote.accesscontrol.facade;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReport;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalView;
import java.util.List;
import java.util.Optional;

/** IP 보안 탐지 근거와 판정에 대한 도메인 외부 공개 계약. */
public interface IpSecuritySignalFacade {
  IpSecuritySignalView report(IpSecuritySignalReport report, Long reporterAdminUserId);

  IpSecuritySignalView review(
      Long signalId, SignalVerdict verdict, String reviewNote, Long reviewerAdminUserId);

  Optional<IpSecuritySignalView> findById(Long signalId);

  List<IpSecuritySignalView> findByIp(String rawIp, int max);
}
