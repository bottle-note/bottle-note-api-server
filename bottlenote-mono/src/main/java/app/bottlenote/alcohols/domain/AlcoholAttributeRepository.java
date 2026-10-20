package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholAttributeRepository {
  AlcoholAttribute save(AlcoholAttribute attribute);

  Optional<AlcoholAttribute> findById(Long id);

  Optional<AlcoholAttribute> findByAttributeKey(String key);

  List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids);

  List<AlcoholAttribute> lockByKeys(Collection<String> keys);

  List<AlcoholAttribute> lockByIds(Collection<Long> ids);

  boolean existsByDisplayNameAndIdNot(String name, Long id);

  OffsetPage<AlcoholAttribute> search(AdminAlcoholAttributeSearchRequest request);

  OffsetPage<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request);

  OffsetPage<Long> findAlcoholIds(AdminAlcoholCompoundSearchRequest request);

  List<AlcoholAttributeKeyCountItem> findKeyStats();

  List<AlcoholAttributeKeywordCountItem> findKeywordStats();

  void delete(AlcoholAttribute attribute);

  void deleteAllByIdIn(Collection<Long> ids);
}
