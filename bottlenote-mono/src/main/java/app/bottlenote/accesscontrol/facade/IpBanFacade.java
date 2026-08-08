package app.bottlenote.accesscontrol.facade;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanCommandResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** IP 차단 상태·감사 이력에 대한 도메인 외부 공개 계약. */
public interface IpBanFacade {

  IpBanCommandResponse ban(String rawIp, Duration ttl, String reason, Long adminUserId);

  IpBanCommandResponse unban(String rawIp, String reason, Long adminUserId);

  IpBanCommandResponse expire(String rawIp, String reason);

  Optional<IpBanDetailResponse> findByIp(String rawIp);

  Optional<IpBanDetailResponse> findById(Long id);

  List<IpBanSummaryResponse> list(IpBanStatus status, int max);
}
