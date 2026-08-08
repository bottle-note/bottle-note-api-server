package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.domain.IpBanEvent;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaIpBanEventRepository
    extends IpBanEventRepository, JpaRepository<IpBanEvent, Long> {

  List<IpBanEvent> findByIpBanIdOrderByIdAsc(Long ipBanId);
}
