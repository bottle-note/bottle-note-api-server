package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@JpaRepositoryImpl
public interface JpaTastingTagRepository
    extends TastingTagRepository, CrudRepository<TastingTag, Long> {

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminTastingTagItem(
        t.id, t.korName, t.engName, t.icon, t.description, t.createAt, t.lastModifyAt
      )
      from tasting_tag t order by t.id asc
      """)
  List<AdminTastingTagItem> findAllTastingTags();
}
