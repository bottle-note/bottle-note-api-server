package app.bottlenote.accesscontrol.domain;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface IpBanRepository {

  IpBan save(IpBan ipBan);

  Optional<IpBan> findById(Long id);

  Optional<IpBan> findByNormalizedIp(String normalizedIp);

  List<IpBan> findByStatusOrderByStateChangedAtDesc(IpBanStatus status, int limit);

  List<IpBan> findAllOrderByStateChangedAtDesc(int limit);

  List<IpBan> findActiveAfter(LocalDateTime expiresAt, Long id, int limit);

  List<IpBan> findTerminatedBefore(LocalDateTime cutoff, int limit);

  int deleteByIds(List<Long> ids);
}
