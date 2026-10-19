package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingKey;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.global.pagination.OffsetPage;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholAttributeMappingRepository
    extends AlcoholAttributeMappingRepository, JpaRepository<AlcoholAttributeMapping, Long> {
  @Override
  default AlcoholAttributeMapping save(AlcoholAttributeMapping mapping) {
    return saveAndFlush(mapping);
  }

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.domain.AlcoholAttributeMappingKey(m.alcoholId, m.attributeId)
      from alcohol_attribute_mapping m
      where m.alcoholId in :alcoholIds and m.attributeId in :attributeIds
      """)
  List<AlcoholAttributeMappingKey> findExistingKeys(
      @Param("alcoholIds") Collection<Long> alcoholIds,
      @Param("attributeIds") Collection<Long> attributeIds);

  @Override
  Optional<AlcoholAttributeMapping> findById(Long id);

  @Override
  boolean existsByAttributeId(Long attributeId);

  List<AlcoholAttributeMapping> findAllByAlcoholIdIn(Collection<Long> ids);

  @Override
  default OffsetPage<AlcoholAttributeMapping> search(
      AdminAlcoholAttributeMappingSearchRequest request) {
    Page<AlcoholAttributeMapping> page =
        selectMappings(
            request.alcoholId(),
            request.attributeId(),
            PageRequest.of(request.page(), request.size(), Sort.by("id")));
    return new OffsetPage<>(page.getContent(), page.getTotalElements());
  }

  @Query(
      """
      select m from alcohol_attribute_mapping m
      where (:alcoholId is null or m.alcoholId = :alcoholId)
        and (:attributeId is null or m.attributeId = :attributeId)
      """)
  Page<AlcoholAttributeMapping> selectMappings(
      @Param("alcoholId") Long alcoholId,
      @Param("attributeId") Long attributeId,
      Pageable pageable);

  // 같은 술의 등록 요청은 DB 잠금으로 직렬화하여 중복 요청도 변경 없이 성공시킨다.
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from alcohol a where a.id in :ids order by a.id")
  List<Alcohol> lockAlcohols(@Param("ids") Collection<Long> ids);

  @Override
  @Modifying(flushAutomatically = true)
  @Query("delete from alcohol_attribute_mapping m where m.attributeId in :ids")
  void deleteAllByAttributeIdIn(@Param("ids") Collection<Long> ids);
}
