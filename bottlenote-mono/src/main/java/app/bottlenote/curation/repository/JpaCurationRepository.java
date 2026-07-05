package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaCurationRepository extends CurationRepository, JpaRepository<Curation, Long> {

  List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  @Override
  @Query(
      """
      SELECT c
      FROM curation c
      WHERE c.isActive = true
        AND (c.exposureStartDate IS NULL OR c.exposureStartDate <= :today)
        AND (c.exposureEndDate IS NULL OR c.exposureEndDate >= :today)
      ORDER BY c.displayOrder ASC, c.id ASC
      """)
  List<Curation> findAllVisibleOn(@Param("today") LocalDate today);

  @Override
  @Query(
      """
      SELECT c
      FROM curation c
      WHERE c.id = :id
        AND c.isActive = true
        AND (c.exposureStartDate IS NULL OR c.exposureStartDate <= :today)
        AND (c.exposureEndDate IS NULL OR c.exposureEndDate >= :today)
      """)
  Optional<Curation> findVisibleById(@Param("id") Long id, @Param("today") LocalDate today);

  @Query(
      """
      SELECT c
      FROM curation c
      WHERE (:keyword IS NULL OR :keyword = '' OR c.name LIKE CONCAT('%', :keyword, '%'))
        AND (:specId IS NULL OR c.specId = :specId)
        AND (:isActive IS NULL OR c.isActive = :isActive)
      ORDER BY c.displayOrder ASC, c.id ASC
      """)
  Page<Curation> searchForAdmin(String keyword, Long specId, Boolean isActive, Pageable pageable);
}
