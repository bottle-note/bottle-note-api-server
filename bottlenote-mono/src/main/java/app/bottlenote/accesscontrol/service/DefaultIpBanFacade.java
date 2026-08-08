package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanDetail;
import app.bottlenote.accesscontrol.dto.response.IpBanSummary;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import app.bottlenote.common.annotation.FacadeService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultIpBanFacade implements IpBanFacade {

  private final IpBanService ipBanService;

  @Override
  public IpBanDetail ban(String rawIp, Duration ttl, String reason, Long adminUserId) {
    return ipBanService.ban(rawIp, ttl, reason, adminUserId);
  }

  @Override
  public IpBanDetail unban(String rawIp, String reason, Long adminUserId) {
    return ipBanService.unban(rawIp, reason, adminUserId);
  }

  @Override
  public IpBanDetail expire(String rawIp, String reason) {
    return ipBanService.expire(rawIp, reason);
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
}
