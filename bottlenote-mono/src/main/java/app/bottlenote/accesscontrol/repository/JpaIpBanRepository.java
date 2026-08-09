package app.bottlenote.accesscontrol.repository;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaIpBanRepository extends IpBanRepository, JpaRepository<IpBan, Long> {

  Optional<IpBan> findByNormalizedIp(String normalizedIp);

  @Override
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from ipBan b where b.normalizedIp = :normalizedIp")
  Optional<IpBan> findByNormalizedIpForUpdate(@Param("normalizedIp") String normalizedIp);

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

  @Query(
      """
      select b from ipBan b
      where b.status = 'ACTIVE'
        and (:expiresAt is null or b.expiresAt > :expiresAt
          or (b.expiresAt = :expiresAt and b.id > :id))
      order by b.expiresAt asc, b.id asc
      """)
  List<IpBan> findActiveAfterQuery(
      @Param("expiresAt") LocalDateTime expiresAt, @Param("id") Long id, Pageable pageable);

  @Override
  default List<IpBan> findActiveAfter(LocalDateTime expiresAt, Long id, int limit) {
    return findActiveAfterQuery(expiresAt, id == null ? 0L : id, pageable(limit));
  }

  @Query(
      """
      select b from ipBan b
      where b.status in ('UNBANNED', 'EXPIRED')
        and (:stateChangedAt is null or b.stateChangedAt > :stateChangedAt
          or (b.stateChangedAt = :stateChangedAt and b.id > :id))
      order by b.stateChangedAt asc, b.id asc
      """)
  List<IpBan> findInactiveAfterQuery(
      @Param("stateChangedAt") LocalDateTime stateChangedAt,
      @Param("id") Long id,
      Pageable pageable);

  @Override
  default List<IpBan> findInactiveAfter(LocalDateTime stateChangedAt, Long id, int limit) {
    return findInactiveAfterQuery(stateChangedAt, id == null ? 0L : id, pageable(limit));
  }

  @Query(
      """
      select b from ipBan b
      where b.status in ('UNBANNED', 'EXPIRED') and b.stateChangedAt < :cutoff
        and not exists (
          select 1 from ipSecuritySignal s
          where s.ipBanId = b.id and s.createAt >= :cutoff
        )
      order by b.stateChangedAt asc, b.id asc
      """)
  List<IpBan> findTerminatedBeforeQuery(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

  @Override
  default List<IpBan> findTerminatedBefore(LocalDateTime cutoff, int limit) {
    return findTerminatedBeforeQuery(cutoff, pageable(limit));
  }

  @Override
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from ipBan b where b.id in :ids")
  int deleteByIds(@Param("ids") List<Long> ids);

  private static Pageable pageable(int limit) {
    int size = Math.max(limit, 1);
    return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "stateChangedAt", "id"));
  }
}
