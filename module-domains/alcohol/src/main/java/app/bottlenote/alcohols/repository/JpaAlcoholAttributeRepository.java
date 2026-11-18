package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@JpaRepositoryImpl
public interface JpaAlcoholAttributeRepository
    extends AlcoholAttributeRepository,
        CustomAlcoholAttributeRepository,
        JpaRepository<AlcoholAttribute, Long> {
  @Override
  default AlcoholAttribute save(AlcoholAttribute attribute) {
    try {
      return saveAndFlush(attribute);
    } catch (DataIntegrityViolationException failure) {
      if (AlcoholAttributeConstraintViolation.isDuplicate(failure)) {
        throw new AlcoholException(AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_DUPLICATE);
      }
      throw failure;
    }
  }

  @Override
  Optional<AlcoholAttribute> findById(Long id);

  @Override
  Optional<AlcoholAttribute> findByAttributeKey(String key);

  List<AlcoholAttribute> findAllByAttributeKeyIn(Collection<String> keys);

  boolean existsByGroupId(Long groupId);

  @Override
  List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from alcohol_attribute a where a.attributeKey in :keys order by a.id")
  List<AlcoholAttribute> lockByKeys(@Param("keys") Collection<String> keys);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from alcohol_attribute a where a.id in :ids order by a.id")
  List<AlcoholAttribute> lockByIds(@Param("ids") Collection<Long> ids);

  @Override
  @Query(
      "select (count(a) > 0) from alcohol_attribute a where a.displayName = :name and (:id is null or a.id <> :id)")
  boolean existsByDisplayNameAndIdNot(@Param("name") String name, @Param("id") Long id);

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem(
        a.attributeKey, a.attributeType, a.datatype, count(distinct a.id), count(distinct m.alcoholId))
      from alcohol_attribute a left join alcohol_attribute_mapping m on m.attributeId = a.id
      group by a.attributeKey, a.attributeType, a.datatype order by a.attributeKey
      """)
  List<AlcoholAttributeKeyCountItem> findKeyStats();

  @Override
  default List<AlcoholAttributeKeywordCountItem> findKeywordStats() {
    return selectKeywordStats(AttributeType.KEYWORD);
  }

  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem(
        m.attributeValue, count(distinct m.alcoholId))
      from alcohol_attribute a join alcohol_attribute_mapping m on m.attributeId = a.id
      where a.attributeType = :type group by m.attributeValue
      order by count(distinct m.alcoholId) desc, m.attributeValue
      """)
  List<AlcoholAttributeKeywordCountItem> selectKeywordStats(@Param("type") AttributeType type);

  @Override
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from alcohol_attribute a where a.id in :ids")
  void deleteAllByIdIn(@Param("ids") Collection<Long> ids);
}
