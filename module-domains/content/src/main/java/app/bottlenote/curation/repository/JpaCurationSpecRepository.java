package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@JpaRepositoryImpl
public interface JpaCurationSpecRepository
    extends CurationSpecRepository, JpaRepository<CurationSpec, Long> {

  Optional<CurationSpec> findByCode(String code);

  List<CurationSpec> findAllByIsActiveTrueOrderByIdAsc();

  @Query(
      """
      select new app.bottlenote.curation.dto.response.CurationSpecListResponse(
        spec.id,
        spec.code,
        spec.name,
        spec.description,
        spec.version,
        spec.isActive
      )
      from curation_spec spec
      where spec.isActive = true
      order by spec.id asc
      """)
  List<CurationSpecListResponse> findAllActiveSpecSummaries();

  List<CurationSpec> findAllByCodeIn(Collection<String> codes);

  List<CurationSpec> findAllByIdIn(Collection<Long> ids);

  boolean existsByCode(String code);
}
