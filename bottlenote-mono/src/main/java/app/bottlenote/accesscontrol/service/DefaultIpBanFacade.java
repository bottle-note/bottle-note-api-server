package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.constant.ProjectionStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanCommandResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@FacadeService
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DefaultIpBanFacade implements IpBanFacade {

  private final IpBanService ipBanService;
  private final AccessControlStore accessControlStore;

  @Override
  public IpBanCommandResponse ban(String rawIp, Duration ttl, String reason, Long adminUserId) {
    IpBanDetailResponse detail = ipBanService.ban(rawIp, ttl, reason, adminUserId);
    return projectBan(detail, ttl);
  }

  @Override
  public IpBanCommandResponse unban(String rawIp, String reason, Long adminUserId) {
    return projectUnban(ipBanService.unban(rawIp, reason, adminUserId));
  }

  @Override
  public IpBanCommandResponse expire(String rawIp, String reason) {
    return projectUnban(ipBanService.expire(rawIp, reason));
  }

  @Override
  public Optional<IpBanDetailResponse> findByIp(String rawIp) {
    return ipBanService.getDetail(rawIp);
  }

  @Override
  public Optional<IpBanDetailResponse> findById(Long id) {
    return ipBanService.getDetail(id);
  }

  @Override
  public List<IpBanSummaryResponse> list(IpBanStatus status, int max) {
    return ipBanService.list(status, max);
  }

  private IpBanCommandResponse projectBan(IpBanDetailResponse detail, Duration ttl) {
    try {
      accessControlStore.projectBan(
          detail.normalizedIp(), ttl, detail.reason(), latestEventId(detail));
      return new IpBanCommandResponse(detail, ProjectionStatus.APPLIED);
    } catch (RuntimeException exception) {
      log.warn(
          "IP ban Redis projection failed ip={} eventId={}",
          detail.normalizedIp(),
          latestEventId(detail),
          exception);
      return new IpBanCommandResponse(detail, ProjectionStatus.PENDING_RECONCILE);
    }
  }

  private IpBanCommandResponse projectUnban(IpBanDetailResponse detail) {
    try {
      accessControlStore.projectUnban(detail.normalizedIp(), latestEventId(detail));
      return new IpBanCommandResponse(detail, ProjectionStatus.APPLIED);
    } catch (RuntimeException exception) {
      log.warn(
          "IP unban Redis projection failed ip={} eventId={}",
          detail.normalizedIp(),
          latestEventId(detail),
          exception);
      return new IpBanCommandResponse(detail, ProjectionStatus.PENDING_RECONCILE);
    }
  }

  private static long latestEventId(IpBanDetailResponse detail) {
    return detail.events().getLast().id();
  }
}
