package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.dto.dsl.AlcoholAttributeSearchCondition;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.common.annotation.DomainRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 위스키 속성 포트. 선택한 위스키들의 속성을 한 번에 읽고, 일괄 추가·삭제한다. */
@DomainRepository
public interface AlcoholAttributeRepository {

  AlcoholAttribute save(AlcoholAttribute attribute);

  List<AlcoholAttribute> saveAll(Collection<AlcoholAttribute> attributes);

  Optional<AlcoholAttribute> findById(Long id);

  List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids);

  List<AlcoholAttribute> findAllByAlcoholIdIn(Collection<Long> alcoholIds);

  List<AlcoholAttribute> findAllByAttributeType(AttributeType attributeType);

  /** 조건에 맞는 속성이 하나라도 있는 위스키 ID를 이름순으로 페이지로 돌려준다 */
  Page<Long> findAlcoholIds(AlcoholAttributeSearchCondition condition, Pageable pageable);

  List<AlcoholAttributeKeyCountItem> findKeyStats();

  List<AlcoholAttributeKeywordCountItem> findKeywordStats();

  boolean existsByAlcoholIdAndAttributeKeyAndAttributeValue(
      Long alcoholId, String attributeKey, String attributeValue);

  boolean existsByAlcoholIdAndAttributeKeyAndAttributeValueAndIdNot(
      Long alcoholId, String attributeKey, String attributeValue, Long id);

  void delete(AlcoholAttribute attribute);

  void deleteAllByIdIn(Collection<Long> ids);
}
