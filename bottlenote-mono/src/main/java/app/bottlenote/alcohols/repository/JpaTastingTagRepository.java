package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaTastingTagRepository
    extends TastingTagRepository, CrudRepository<TastingTag, Long> {

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminTastingTagItem(
        t.id, t.korName, t.engName, t.icon, t.description, t.createAt, t.lastModifyAt
      )
      from tasting_tag t
      where (:keyword is null or :keyword = ''
        or t.korName like concat('%', :keyword, '%')
        or t.engName like concat('%', :keyword, '%'))
      """)
  Page<AdminTastingTagItem> findAllTastingTags(@Param("keyword") String keyword, Pageable pageable);
}
