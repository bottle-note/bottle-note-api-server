package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholAttributeGroup;
import app.bottlenote.alcohols.domain.AlcoholAttributeGroupRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import java.util.*;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholAttributeGroupRepository implements AlcoholAttributeGroupRepository {
  private final Map<Long, AlcoholAttributeGroup> rows = new LinkedHashMap<>();
  private long sequence;
  @Override public List<AlcoholAttributeGroup> findAllByOrderByDisplayOrderAscIdAsc() {
    return rows.values().stream().sorted(Comparator.comparingInt(AlcoholAttributeGroup::getDisplayOrder).thenComparing(AlcoholAttributeGroup::getId)).toList();
  }
  @Override public List<AlcoholAttributeGroup> lockByIds(Collection<Long> ids) {
    return rows.values().stream().filter(g -> ids.contains(g.getId())).sorted(Comparator.comparing(AlcoholAttributeGroup::getId)).toList();
  }
  @Override public boolean existsByNameAndIdNot(String name, Long id) {
    return rows.values().stream().anyMatch(g -> !Objects.equals(g.getId(), id) && folded(g.getName()).equals(folded(name)));
  }
  private String folded(String name) {
    return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }
  @Override public AlcoholAttributeGroup save(AlcoholAttributeGroup group) {
    if (existsByNameAndIdNot(group.getName(), group.getId())) throw new AlcoholException(AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_GROUP_DUPLICATE);
    if (group.getId() == null) ReflectionTestUtils.setField(group, "id", ++sequence);
    rows.put(group.getId(), group);
    return group;
  }
  @Override public void delete(AlcoholAttributeGroup group) { rows.remove(group.getId()); }
}
