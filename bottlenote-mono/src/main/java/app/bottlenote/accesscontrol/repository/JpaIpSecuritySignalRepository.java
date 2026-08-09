package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaIpSecuritySignalRepository
    extends IpSecuritySignalRepository, JpaRepository<IpSecuritySignal, Long> {

  List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, PageRequest pageable);

  @Override
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ipSecuritySignal s where s.id = :id")
  Optional<IpSecuritySignal> findByIdForUpdate(@Param("id") Long id);

  @Override
  default List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, int limit) {
    return findByNormalizedIpOrderByIdDesc(normalizedIp, PageRequest.of(0, Math.max(1, limit)));
  }

  List<IpSecuritySignal> findByCreateAtBeforeOrderByIdAsc(
      LocalDateTime cutoff, PageRequest pageable);

  @Override
  default List<IpSecuritySignal> findByCreateAtBeforeOrderByIdAsc(LocalDateTime cutoff, int limit) {
    return findByCreateAtBeforeOrderByIdAsc(cutoff, PageRequest.of(0, Math.max(1, limit)));
  }

  @Override
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ipSecuritySignal s where s.id in :ids")
  int deleteByIds(@Param("ids") List<Long> ids);

  @Override
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ipSecuritySignal s where s.ipBanId in :ipBanIds")
  int deleteByIpBanIdIn(@Param("ipBanIds") List<Long> ipBanIds);
}
