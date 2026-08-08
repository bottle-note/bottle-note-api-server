package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanCommandResult;
import app.bottlenote.accesscontrol.dto.response.IpBanDetail;
import app.bottlenote.accesscontrol.dto.response.IpBanSummary;
import app.bottlenote.accesscontrol.dto.response.ProjectionStatus;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@FacadeService
@Slf4j
@RequiredArgsConstructor
public class DefaultIpBanFacade implements IpBanFacade {

  private final IpBanService ipBanService;
  private final AccessControlStore accessControlStore;

  @Override
  public IpBanCommandResult ban(String rawIp, Duration ttl, String reason, Long adminUserId) {
    IpBanDetail detail = ipBanService.ban(rawIp, ttl, reason, adminUserId);
    return projectBan(detail, ttl);
  }

  @Override
  public IpBanCommandResult unban(String rawIp, String reason, Long adminUserId) {
    return projectUnban(ipBanService.unban(rawIp, reason, adminUserId));
  }

  @Override
  public IpBanCommandResult expire(String rawIp, String reason) {
    return projectUnban(ipBanService.expire(rawIp, reason));
  }

  @Override
  public Optional<IpBanDetail> findByIp(String rawIp) {
    return ipBanService.getDetail(rawIp);
  }

  @Override
  public Optional<IpBanDetail> findById(Long id) {
    return ipBanService.getDetail(id);
  }

  @Override
  public List<IpBanSummary> list(IpBanStatus status, int max) {
    return ipBanService.list(status, max);
  }

  private IpBanCommandResult projectBan(IpBanDetail detail, Duration ttl) {
    try {
      accessControlStore.projectBan(
          detail.normalizedIp(), ttl, detail.reason(), latestEventId(detail));
      return new IpBanCommandResult(detail, ProjectionStatus.APPLIED);
    } catch (RuntimeException exception) {
      log.warn(
          "IP ban Redis projection failed ip={} eventId={}",
          detail.normalizedIp(),
          latestEventId(detail),
          exception);
      return new IpBanCommandResult(detail, ProjectionStatus.PENDING_RECONCILE);
    }
  }

  private IpBanCommandResult projectUnban(IpBanDetail detail) {
    try {
      accessControlStore.projectUnban(detail.normalizedIp(), latestEventId(detail));
      return new IpBanCommandResult(detail, ProjectionStatus.APPLIED);
    } catch (RuntimeException exception) {
      log.warn(
          "IP unban Redis projection failed ip={} eventId={}",
          detail.normalizedIp(),
          latestEventId(detail),
          exception);
      return new IpBanCommandResult(detail, ProjectionStatus.PENDING_RECONCILE);
    }
  }

  private static long latestEventId(IpBanDetail detail) {
    return detail.events().getLast().id();
  }
}
