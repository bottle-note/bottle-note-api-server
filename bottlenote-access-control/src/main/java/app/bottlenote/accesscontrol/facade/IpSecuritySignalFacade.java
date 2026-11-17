package app.bottlenote.accesscontrol.facade;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReportRequest;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalResponse;
import java.util.List;
import java.util.Optional;

/** IP 보안 탐지 근거와 판정에 대한 도메인 외부 공개 계약. */
public interface IpSecuritySignalFacade {
  IpSecuritySignalResponse report(IpSecuritySignalReportRequest report, Long reporterAdminUserId);

  IpSecuritySignalResponse review(
      Long signalId, SignalVerdict verdict, String reviewNote, Long reviewerAdminUserId);

  Optional<IpSecuritySignalResponse> findById(Long signalId);

  List<IpSecuritySignalResponse> findByIp(String rawIp, int max);
}
