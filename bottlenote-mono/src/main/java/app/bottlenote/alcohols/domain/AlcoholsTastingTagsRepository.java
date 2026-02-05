package app.bottlenote.alcohols.domain;

import java.util.List;

public interface AlcoholsTastingTagsRepository {

  List<AlcoholsTastingTags> findByTastingTagId(Long tastingTagId);

  <S extends AlcoholsTastingTags> List<S> saveAll(Iterable<S> alcoholsTastingTags);

  void deleteByTastingTagIdAndAlcoholIdIn(Long tastingTagId, List<Long> alcoholIds);

  boolean existsByTastingTagId(Long tastingTagId);
}
