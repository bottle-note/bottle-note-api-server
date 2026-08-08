package app.bottlenote.accesscontrol.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface IpSecuritySignalRepository {
  IpSecuritySignal save(IpSecuritySignal signal);

  Optional<IpSecuritySignal> findById(Long id);

  List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, int limit);
}
