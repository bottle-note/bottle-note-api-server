package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@JpaRepositoryImpl
public interface JpaAlcoholAttributeRepository
    extends AlcoholAttributeRepository,
        CustomAlcoholAttributeRepository,
        JpaRepository<AlcoholAttribute, Long> {

  // 수정은 커밋 때 flush되므로, 유니크 위반을 저장 호출 안에서 받기 위해 바로 반영한다
  @Override
  default AlcoholAttribute save(AlcoholAttribute attribute) {
    return saveAndFlush(attribute);
  }

  @Override
  default List<AlcoholAttribute> saveAll(Collection<AlcoholAttribute> attributes) {
    return saveAll((Iterable<AlcoholAttribute>) attributes);
  }

  @Override
  Optional<AlcoholAttribute> findById(Long id);

  // 빈 id 목록은 IN () SQL이 되지 않게 쿼리를 건너뛴다
  @Override
  default List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return selectAllByIdIn(ids);
  }

  @Query(
      """
      select a from alcohol_attribute a
      where a.id in :ids
      order by a.id asc
      """)
  List<AlcoholAttribute> selectAllByIdIn(@Param("ids") Collection<Long> ids);

  @Override
  @Query(
      """
      select a from alcohol_attribute a
      where a.alcoholId in :alcoholIds
      order by a.alcoholId asc, a.id asc
      """)
  List<AlcoholAttribute> findAllByAlcoholIdIn(@Param("alcoholIds") Collection<Long> alcoholIds);

  @Override
  List<AlcoholAttribute> findAllByAttributeType(AttributeType attributeType);

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem(
        a.attributeKey, a.attributeType, a.datatype, count(a.id), count(distinct a.alcoholId)
      )
      from alcohol_attribute a
      group by a.attributeKey, a.attributeType, a.datatype
      order by a.attributeKey asc, a.attributeType asc, a.datatype asc
      """)
  List<AlcoholAttributeKeyCountItem> findKeyStats();

  @Override
  default List<AlcoholAttributeKeywordCountItem> findKeywordStats() {
    return selectKeywordStats(AttributeType.KEYWORD);
  }

  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem(
        a.attributeValue, count(distinct a.alcoholId)
      )
      from alcohol_attribute a
      where a.attributeType = :attributeType
      group by a.attributeValue
      order by count(distinct a.alcoholId) desc, a.attributeValue asc
      """)
  List<AlcoholAttributeKeywordCountItem> selectKeywordStats(
      @Param("attributeType") AttributeType attributeType);

  @Override
  boolean existsByAlcoholIdAndAttributeKeyAndAttributeValue(
      Long alcoholId, String attributeKey, String attributeValue);

  @Override
  boolean existsByAlcoholIdAndAttributeKeyAndAttributeValueAndIdNot(
      Long alcoholId, String attributeKey, String attributeValue, Long id);

  // 벌크 삭제 JPQL은 트랜잭션이 필요하다. 서비스 밖에서 호출돼도 동작하도록 여기서 연다
  @Override
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from alcohol_attribute a where a.id in :ids")
  void deleteAllByIdIn(@Param("ids") Collection<Long> ids);
}
