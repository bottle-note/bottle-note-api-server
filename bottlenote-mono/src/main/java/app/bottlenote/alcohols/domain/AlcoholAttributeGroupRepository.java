package app.bottlenote.alcohols.domain;

import java.util.Collection;
import java.util.List;

public interface AlcoholAttributeGroupRepository {
  List<AlcoholAttributeGroup> findAllByOrderByDisplayOrderAscIdAsc();

  List<AlcoholAttributeGroup> lockByIds(Collection<Long> ids);

  boolean existsByNameAndIdNot(String name, Long id);

  AlcoholAttributeGroup save(AlcoholAttributeGroup group);

  void delete(AlcoholAttributeGroup group);
}
