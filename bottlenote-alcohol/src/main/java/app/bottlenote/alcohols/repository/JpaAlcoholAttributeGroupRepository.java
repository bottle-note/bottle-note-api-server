package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_GROUP_DUPLICATE;

import app.bottlenote.alcohols.domain.AlcoholAttributeGroup;
import app.bottlenote.alcohols.domain.AlcoholAttributeGroupRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholAttributeGroupRepository
    extends AlcoholAttributeGroupRepository, JpaRepository<AlcoholAttributeGroup, Long> {
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from alcohol_attribute_group g where g.id in :ids order by g.id")
  List<AlcoholAttributeGroup> lockByIds(@Param("ids") Collection<Long> ids);

  @Override
  @Query(
      "select (count(g) > 0) from alcohol_attribute_group g where g.name = :name and (:id is null or g.id <> :id)")
  boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Long id);

  @Override
  default AlcoholAttributeGroup save(AlcoholAttributeGroup group) {
    try {
      return saveAndFlush(group);
    } catch (DataIntegrityViolationException failure) {
      if (AlcoholAttributeConstraintViolation.isDuplicate(
          failure, java.util.Set.of("uk_alcohol_attribute_groups_name")))
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_DUPLICATE);
      throw failure;
    }
  }
}
