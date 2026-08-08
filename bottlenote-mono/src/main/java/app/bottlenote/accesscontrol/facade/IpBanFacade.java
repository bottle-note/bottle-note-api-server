package app.bottlenote.accesscontrol.facade;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanDetail;
import app.bottlenote.accesscontrol.dto.response.IpBanSummary;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** IP 차단 상태·감사 이력에 대한 도메인 외부 공개 계약. */
public interface IpBanFacade {

  IpBanDetail ban(String rawIp, Duration ttl, String reason, Long adminUserId);

  IpBanDetail unban(String rawIp, String reason, Long adminUserId);

  IpBanDetail expire(String rawIp, String reason);

  Optional<IpBanDetail> findByIp(String rawIp);

  Optional<IpBanDetail> findById(Long id);

  List<IpBanSummary> list(IpBanStatus status, int max);
}
