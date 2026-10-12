package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.TastingTagAlcoholCountItem;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AlcoholsTastingTagsRepository {

  List<AlcoholsTastingTags> findByTastingTagId(Long tastingTagId);

  Set<Long> findAlcoholIdsByTastingTagId(Long tastingTagId);

  <S extends AlcoholsTastingTags> List<S> saveAll(Iterable<S> alcoholsTastingTags);

  void deleteByTastingTagIdAndAlcoholIdIn(Long tastingTagId, List<Long> alcoholIds);

  boolean existsByTastingTagId(Long tastingTagId);

  void deleteByAlcoholId(Long alcoholId);

  /** 태그별 위스키 수. 매핑이 없는 태그는 결과에 나오지 않는다 */
  List<TastingTagAlcoholCountItem> countAlcoholsByTastingTagIds(Collection<Long> tastingTagIds);
}
