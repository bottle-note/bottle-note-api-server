package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaIpBanRepository extends IpBanRepository, JpaRepository<IpBan, Long> {

  Optional<IpBan> findByNormalizedIp(String normalizedIp);

  @Query("select b from ipBan b where b.status = :status order by b.stateChangedAt desc, b.id desc")
  List<IpBan> findByStatusOrdered(@Param("status") IpBanStatus status, Pageable pageable);

  @Query("select b from ipBan b order by b.stateChangedAt desc, b.id desc")
  List<IpBan> findAllOrdered(Pageable pageable);

  @Override
  default List<IpBan> findByStatusOrderByStateChangedAtDesc(IpBanStatus status, int limit) {
    return findByStatusOrdered(status, pageable(limit));
  }

  @Override
  default List<IpBan> findAllOrderByStateChangedAtDesc(int limit) {
    return findAllOrdered(pageable(limit));
  }

  private static Pageable pageable(int limit) {
    int size = Math.max(limit, 1);
    return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "stateChangedAt", "id"));
  }
}
