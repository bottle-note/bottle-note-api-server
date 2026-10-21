package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholAttributeMappingRepository {
  AlcoholAttributeMapping save(AlcoholAttributeMapping mapping);

  List<AlcoholAttributeMappingKey> findExistingKeys(
      Collection<Long> alcoholIds, Collection<Long> attributeIds);

  Optional<AlcoholAttributeMapping> findById(Long id);

  boolean existsByAttributeId(Long attributeId);

  boolean hasMultipleValues(Long attributeId);

  boolean existsEquivalentMapping(Long alcoholId, Long attributeId, String value, Long excludedId);

  List<String> findValuesByAttributeId(Long attributeId);

  List<AlcoholAttributeMapping> findAllByAlcoholIdIn(Collection<Long> ids);

  OffsetPage<AlcoholAttributeMapping> search(AdminAlcoholAttributeMappingSearchRequest request);

  List<Alcohol> lockAlcohols(Collection<Long> ids);

  void delete(AlcoholAttributeMapping mapping);

  void deleteAllByAttributeIdIn(Collection<Long> ids);
}
