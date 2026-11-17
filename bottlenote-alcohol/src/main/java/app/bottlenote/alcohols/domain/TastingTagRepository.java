package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.TastingTagCatalogSourceItem;
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TastingTagRepository {

  List<TastingTag> findAll();

  /** 아이콘 컬럼을 제외한 전체 태그. */
  List<TastingTagCatalogSourceItem> findAllCatalogSources();

  Page<TastingTagNodeItem> findAllTastingTags(String keyword, Pageable pageable);

  Optional<TastingTag> findById(Long id);

  Optional<TastingTag> findByKorName(String korName);

  List<TastingTag> findByParentId(Long parentId);

  TastingTag save(TastingTag tastingTag);

  void delete(TastingTag tastingTag);

  boolean existsByKorNameAndIdNot(String korName, Long id);

  boolean existsByParentId(Long parentId);

  void lockTastingTags(java.util.Collection<Long> ids);
}
