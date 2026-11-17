package app.bottlenote.curation.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface CurationSpecRepository {

  Optional<CurationSpec> findById(Long id);

  Optional<CurationSpec> findByIdAndIsActiveTrue(Long id);

  Optional<CurationSpec> findByCode(String code);

  List<CurationSpec> findAllByIsActiveTrueOrderByIdAsc();

  List<CurationSpecListResponse> findAllActiveSpecSummaries();

  List<CurationSpec> findAllByCodeIn(Collection<String> codes);

  List<CurationSpec> findAllByIdIn(Collection<Long> ids);

  boolean existsByCode(String code);

  CurationSpec save(CurationSpec curationSpec);
}
