package app.bottlenote.accesscontrol.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;

@DomainRepository
public interface IpBanEventRepository {

  IpBanAuditRecord save(IpBanAuditRecord event);

  List<IpBanAuditRecord> findByIpBanIdOrderByIdAsc(Long ipBanId);

  long findLatestIdByIpBanId(Long ipBanId);

  int deleteByIpBanIdIn(List<Long> ipBanIds);
}
