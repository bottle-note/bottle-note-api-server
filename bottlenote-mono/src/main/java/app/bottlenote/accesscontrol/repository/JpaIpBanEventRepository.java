package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaIpBanEventRepository
    extends IpBanEventRepository, JpaRepository<IpBanAuditRecord, Long> {

  List<IpBanAuditRecord> findByIpBanIdOrderByIdAsc(Long ipBanId);

  @Override
  @Query("select coalesce(max(e.id), 0) from ipBanEvent e where e.ipBanId = :ipBanId")
  long findLatestIdByIpBanId(@Param("ipBanId") Long ipBanId);

  @Override
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ipBanEvent e where e.ipBanId in :ipBanIds")
  int deleteByIpBanIdIn(@Param("ipBanIds") List<Long> ipBanIds);
}
