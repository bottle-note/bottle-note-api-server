package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/** 맛 그룹 포트의 상태 기반 테스트 더블 */
public class InMemoryTastingTagGroupRepository implements TastingTagGroupRepository {

  private final List<TastingTagGroup> groups = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public List<TastingTagGroup> findAllWithTags() {
    return groups.stream()
        .sorted(
            Comparator.comparing(
                    TastingTagGroup::getFamily, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TastingTagGroup::getDisplayOrder)
                .thenComparing(TastingTagGroup::getId))
        .toList();
  }

  @Override
  public Optional<TastingTagGroup> findById(Long id) {
    return groups.stream().filter(group -> Objects.equals(group.getId(), id)).findFirst();
  }

  @Override
  public Optional<TastingTagGroup> findByCode(String code) {
    return groups.stream().filter(group -> Objects.equals(group.getCode(), code)).findFirst();
  }

  @Override
  public boolean existsByCode(String code) {
    return findByCode(code).isPresent();
  }

  @Override
  public boolean existsByName(String name) {
    return groups.stream().anyMatch(group -> Objects.equals(group.getName(), name));
  }

  @Override
  public boolean existsByNameAndIdNot(String name, Long id) {
    return groups.stream()
        .anyMatch(
            group ->
                Objects.equals(group.getName(), name) && !Objects.equals(group.getId(), id));
  }

  @Override
  public TastingTagGroup save(TastingTagGroup group) {
    if (group.getId() == null) {
      ReflectionTestUtils.setField(group, "id", sequence.incrementAndGet());
    }
    groups.removeIf(existing -> Objects.equals(existing.getId(), group.getId()));
    groups.add(group);
    return group;
  }

  @Override
  public void delete(TastingTagGroup group) {
    groups.removeIf(existing -> Objects.equals(existing.getId(), group.getId()));
  }
}
