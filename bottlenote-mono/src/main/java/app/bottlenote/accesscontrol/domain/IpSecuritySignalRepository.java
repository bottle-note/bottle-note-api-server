package app.bottlenote.accesscontrol.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface IpSecuritySignalRepository {
  IpSecuritySignal save(IpSecuritySignal signal);

  Optional<IpSecuritySignal> findById(Long id);

  Optional<IpSecuritySignal> findByIdForUpdate(Long id);

  List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, int limit);

  List<IpSecuritySignal> findByCreateAtBeforeOrderByIdAsc(LocalDateTime cutoff, int limit);

  int deleteByIds(List<Long> ids);

  int deleteByIpBanIdIn(List<Long> ipBanIds);
}
