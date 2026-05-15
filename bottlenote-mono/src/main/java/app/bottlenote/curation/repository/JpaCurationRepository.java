package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaCurationRepository extends CurationRepository, JpaRepository<Curation, Long> {

  List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  @Query(
      """
      SELECT c
      FROM curation c
      WHERE (:keyword IS NULL OR :keyword = '' OR c.name LIKE CONCAT('%', :keyword, '%'))
        AND (:isActive IS NULL OR c.isActive = :isActive)
      ORDER BY c.displayOrder ASC, c.id ASC
      """)
  Page<Curation> searchForAdmin(String keyword, Boolean isActive, Pageable pageable);
}
