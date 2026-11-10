package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholMergeLog;
import app.bottlenote.alcohols.domain.AlcoholMergeRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.global.pagination.OffsetPage;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholMergeRepository
    extends AlcoholMergeRepository, JpaRepository<AlcoholMergeLog, Long> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from alcohol a where a.id in :alcoholIds order by a.id")
  List<Alcohol> lockAlcohols(@Param("alcoholIds") List<Long> alcoholIds);

  @Override
  @Modifying
  @Query("delete from alcohol a where a.id = :alcoholId")
  void deleteAlcohol(@Param("alcoholId") Long alcoholId);

  @Query(
      """
      select l from alcohol_merge_log l
      where (:targetAlcoholId is null or l.targetAlcoholId = :targetAlcoholId)
        and (:sourceAlcoholId is null or l.sourceAlcoholId = :sourceAlcoholId)
      order by l.id desc
      """)
  Page<AlcoholMergeLog> findLogs(
      @Param("targetAlcoholId") Long targetAlcoholId,
      @Param("sourceAlcoholId") Long sourceAlcoholId,
      Pageable pageable);

  @Override
  default OffsetPage<AlcoholMergeLog> searchLogs(
      Long targetAlcoholId, Long sourceAlcoholId, int page, int size) {
    Page<AlcoholMergeLog> logs =
        findLogs(targetAlcoholId, sourceAlcoholId, PageRequest.of(page, size));
    return new OffsetPage<>(logs.getContent(), logs.getTotalElements());
  }
}
