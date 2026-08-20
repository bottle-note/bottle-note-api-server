package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaCurationRepository
    extends CurationRepository,
        JpaRepository<Curation, Long>,
        CustomCurationFeedRepository,
        CustomCurationAdminRepository {

  @Override
  @Query(
      """
      SELECT c FROM curation c
      WHERE c.isActive = true
      ORDER BY CASE WHEN c.exposureStartDate IS NULL THEN 1 ELSE 0 END ASC,
               c.exposureStartDate DESC, c.id DESC
      """)
  List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  List<Curation> findAllByIdIn(Collection<Long> ids);

  @Override
  @Query(
      """
      SELECT c FROM curation c
      WHERE c.isActive = true
        AND (c.exposureStartDate IS NULL OR c.exposureStartDate <= :today)
        AND (c.exposureEndDate IS NULL OR c.exposureEndDate >= :today)
      ORDER BY CASE WHEN c.exposureStartDate IS NULL THEN 1 ELSE 0 END ASC,
               c.exposureStartDate DESC, c.id DESC
      """)
  List<Curation> findAllVisibleOn(@Param("today") LocalDate today);

  @Override
  @Query(
      """
      SELECT c FROM curation c
      WHERE c.id = :id AND c.isActive = true
        AND (c.exposureStartDate IS NULL OR c.exposureStartDate <= :today)
        AND (c.exposureEndDate IS NULL OR c.exposureEndDate >= :today)
      """)
  Optional<Curation> findVisibleById(@Param("id") Long id, @Param("today") LocalDate today);
}
