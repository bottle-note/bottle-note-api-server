package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholsTastingTagsRepository implements AlcoholsTastingTagsRepository {

  private final List<AlcoholsTastingTags> mappings = new ArrayList<>();
  private final AtomicLong idSequence = new AtomicLong(1);

  @Override
  public List<AlcoholsTastingTags> findByTastingTagId(Long tastingTagId) {
    return mappings.stream()
        .filter(att -> Objects.equals(att.getTastingTag().getId(), tastingTagId))
        .toList();
  }

  @Override
  public Set<Long> findAlcoholIdsByTastingTagId(Long tastingTagId) {
    return mappings.stream()
        .filter(att -> Objects.equals(att.getTastingTag().getId(), tastingTagId))
        .map(att -> att.getAlcohol().getId())
        .collect(Collectors.toSet());
  }

  @Override
  public <S extends AlcoholsTastingTags> List<S> saveAll(Iterable<S> alcoholsTastingTags) {
    List<S> saved = new ArrayList<>();
    for (S mapping : alcoholsTastingTags) {
      Long alcoholId = mapping.getAlcohol().getId();
      Long tagId = mapping.getTastingTag().getId();
      boolean duplicate =
          mappings.stream()
              .anyMatch(
                  existing ->
                      Objects.equals(existing.getAlcohol().getId(), alcoholId)
                          && Objects.equals(existing.getTastingTag().getId(), tagId));
      if (duplicate) {
        throw new DataIntegrityViolationException(
            "duplicate alcohol-tasting-tag mapping: " + alcoholId + "/" + tagId);
      }
      if (mapping.getId() == null) {
        ReflectionTestUtils.setField(mapping, "id", idSequence.getAndIncrement());
      }
      mappings.add(mapping);
      saved.add(mapping);
    }
    return saved;
  }

  @Override
  public void deleteByTastingTagIdAndAlcoholIdIn(Long tastingTagId, List<Long> alcoholIds) {
    mappings.removeIf(
        att ->
            Objects.equals(att.getTastingTag().getId(), tastingTagId)
                && alcoholIds.contains(att.getAlcohol().getId()));
  }

  @Override
  public boolean existsByTastingTagId(Long tastingTagId) {
    return mappings.stream()
        .anyMatch(att -> Objects.equals(att.getTastingTag().getId(), tastingTagId));
  }

  @Override
  public void deleteByAlcoholId(Long alcoholId) {
    mappings.removeIf(att -> Objects.equals(att.getAlcohol().getId(), alcoholId));
  }

  public List<AlcoholsTastingTags> findAll() {
    return List.copyOf(mappings);
  }

  public int count() {
    return mappings.size();
  }

  public void clear() {
    mappings.clear();
  }
}
