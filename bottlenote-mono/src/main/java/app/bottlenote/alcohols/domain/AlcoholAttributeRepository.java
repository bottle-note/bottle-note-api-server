package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.common.annotation.DomainRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 위스키 속성 포트. 선택한 위스키들의 속성을 한 번에 읽고, 일괄 추가·삭제한다. */
@DomainRepository
public interface AlcoholAttributeRepository {

  AlcoholAttribute save(AlcoholAttribute attribute);

  List<AlcoholAttribute> saveAll(Collection<AlcoholAttribute> attributes);

  Optional<AlcoholAttribute> findById(Long id);

  List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids);

  List<AlcoholAttribute> findAllByAlcoholIdIn(Collection<Long> alcoholIds);

  List<AlcoholAttribute> findAllByAttributeType(AttributeType attributeType);

  boolean existsByAlcoholIdAndAttributeKeyAndAttributeValue(
      Long alcoholId, String attributeKey, String attributeValue);

  void delete(AlcoholAttribute attribute);

  void deleteAllByIdIn(Collection<Long> ids);
}
