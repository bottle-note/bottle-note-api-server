package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TastingTagRepository {

  List<TastingTag> findAll();

  Page<AdminTastingTagItem> findAllTastingTags(String keyword, Pageable pageable);

  Optional<TastingTag> findById(Long id);

  Optional<TastingTag> findByKorName(String korName);

  List<TastingTag> findByParentId(Long parentId);

  TastingTag save(TastingTag tastingTag);

  void delete(TastingTag tastingTag);

  boolean existsByKorNameAndIdNot(String korName, Long id);

  boolean existsByParentId(Long parentId);
}
