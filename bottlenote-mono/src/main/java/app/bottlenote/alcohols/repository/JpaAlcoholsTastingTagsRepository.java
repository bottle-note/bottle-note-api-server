package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholsTastingTagsRepository
    extends AlcoholsTastingTagsRepository, JpaRepository<AlcoholsTastingTags, Long> {

  @Override
  @Query("select att from alcohol_tasting_tags att where att.tastingTag.id = :tastingTagId")
  List<AlcoholsTastingTags> findByTastingTagId(@Param("tastingTagId") Long tastingTagId);

  @Override
  @Modifying
  @Query(
      """
      delete from alcohol_tasting_tags att
      where att.tastingTag.id = :tastingTagId and att.alcohol.id in :alcoholIds
      """)
  void deleteByTastingTagIdAndAlcoholIdIn(
      @Param("tastingTagId") Long tastingTagId, @Param("alcoholIds") List<Long> alcoholIds);

  @Override
  @Query(
      "select case when count(att) > 0 then true else false end from alcohol_tasting_tags att where att.tastingTag.id = :tastingTagId")
  boolean existsByTastingTagId(@Param("tastingTagId") Long tastingTagId);
}
