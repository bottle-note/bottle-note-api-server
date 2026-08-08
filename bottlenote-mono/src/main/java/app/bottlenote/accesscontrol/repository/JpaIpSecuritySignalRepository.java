package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaIpSecuritySignalRepository
    extends IpSecuritySignalRepository, JpaRepository<IpSecuritySignal, Long> {

  List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, PageRequest pageable);

  @Override
  default List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, int limit) {
    return findByNormalizedIpOrderByIdDesc(normalizedIp, PageRequest.of(0, Math.max(1, limit)));
  }
}
