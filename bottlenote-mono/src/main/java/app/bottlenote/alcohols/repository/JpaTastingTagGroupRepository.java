package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaTastingTagGroupRepository
    extends TastingTagGroupRepository, JpaRepository<TastingTagGroup, Long> {

  @Override
  @Query(
      """
      select distinct g from tasting_tag_group g
      left join fetch g.tags
      order by g.family asc, g.displayOrder asc, g.id asc
      """)
  List<TastingTagGroup> findAllWithTags();

  @Override
  @Query("select g from tasting_tag_group g left join fetch g.tags where g.id = :id")
  Optional<TastingTagGroup> findById(@Param("id") Long id);

  @Override
  @Query("select g from tasting_tag_group g left join fetch g.tags where g.code = :code")
  Optional<TastingTagGroup> findByCode(@Param("code") String code);

  @Override
  boolean existsByCode(String code);

  @Override
  boolean existsByNameAndIdNot(String name, Long id);
}
