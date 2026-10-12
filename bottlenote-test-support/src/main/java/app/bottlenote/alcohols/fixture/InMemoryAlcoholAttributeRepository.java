package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/** 위스키 속성 포트의 상태 기반 테스트 더블 */
public class InMemoryAlcoholAttributeRepository implements AlcoholAttributeRepository {

  private final List<AlcoholAttribute> rows = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public AlcoholAttribute save(AlcoholAttribute attribute) {
    if (attribute.getId() == null) {
      ReflectionTestUtils.setField(attribute, "id", sequence.incrementAndGet());
    }
    rows.removeIf(row -> Objects.equals(row.getId(), attribute.getId()));
    rows.add(attribute);
    return attribute;
  }

  @Override
  public List<AlcoholAttribute> saveAll(Collection<AlcoholAttribute> attributes) {
    return attributes.stream().map(this::save).toList();
  }

  @Override
  public Optional<AlcoholAttribute> findById(Long id) {
    return rows.stream().filter(row -> Objects.equals(row.getId(), id)).findFirst();
  }

  @Override
  public List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    Set<Long> targets = new HashSet<>();
    for (Long id : ids) {
      if (id != null) {
        targets.add(id);
      }
    }
    return rows.stream()
        .filter(row -> targets.contains(row.getId()))
        .sorted(Comparator.comparing(AlcoholAttribute::getId))
        .toList();
  }

  @Override
  public List<AlcoholAttribute> findAllByAlcoholIdIn(Collection<Long> alcoholIds) {
    Set<Long> ids = Set.copyOf(alcoholIds);
    return rows.stream()
        .filter(row -> ids.contains(row.getAlcoholId()))
        .sorted(
            Comparator.comparing(AlcoholAttribute::getAlcoholId)
                .thenComparing(AlcoholAttribute::getId))
        .toList();
  }

  @Override
  public List<AlcoholAttribute> findAllByAttributeType(AttributeType attributeType) {
    return rows.stream().filter(row -> row.getAttributeType() == attributeType).toList();
  }

  @Override
  public List<AlcoholAttributeKeyCountItem> findKeyStats() {
    Map<KeyGroup, List<AlcoholAttribute>> grouped = new HashMap<>();
    for (AlcoholAttribute row : rows) {
      grouped
          .computeIfAbsent(
              new KeyGroup(row.getAttributeKey(), row.getAttributeType(), row.getDatatype()),
              key -> new ArrayList<>())
          .add(row);
    }
    return grouped.entrySet().stream()
        .map(
            entry ->
                new AlcoholAttributeKeyCountItem(
                    entry.getKey().key(),
                    entry.getKey().type(),
                    entry.getKey().datatype(),
                    (long) entry.getValue().size(),
                    entry.getValue().stream().map(AlcoholAttribute::getAlcoholId).distinct().count()))
        .sorted(
            Comparator.comparing(AlcoholAttributeKeyCountItem::key)
                .thenComparing(item -> item.type().name())
                .thenComparing(item -> item.datatype().name()))
        .toList();
  }

  @Override
  public List<AlcoholAttributeKeywordCountItem> findKeywordStats() {
    Map<String, Set<Long>> alcoholsByValue = new HashMap<>();
    for (AlcoholAttribute row : rows) {
      if (row.getAttributeType() != AttributeType.KEYWORD) {
        continue;
      }
      alcoholsByValue
          .computeIfAbsent(row.getAttributeValue(), value -> new HashSet<>())
          .add(row.getAlcoholId());
    }
    return alcoholsByValue.entrySet().stream()
        .map(entry -> new AlcoholAttributeKeywordCountItem(entry.getKey(), (long) entry.getValue().size()))
        .sorted(
            Comparator.comparingLong(AlcoholAttributeKeywordCountItem::alcoholCount)
                .reversed()
                .thenComparing(AlcoholAttributeKeywordCountItem::value))
        .toList();
  }

  @Override
  public boolean existsByAlcoholIdAndAttributeKeyAndAttributeValue(
      Long alcoholId, String attributeKey, String attributeValue) {
    return rows.stream()
        .anyMatch(
            row ->
                Objects.equals(row.getAlcoholId(), alcoholId)
                    && row.hasSameKeyAndValue(attributeKey, attributeValue));
  }

  @Override
  public boolean existsByAlcoholIdAndAttributeKeyAndAttributeValueAndIdNot(
      Long alcoholId, String attributeKey, String attributeValue, Long id) {
    return rows.stream()
        .anyMatch(
            row ->
                !Objects.equals(row.getId(), id)
                    && Objects.equals(row.getAlcoholId(), alcoholId)
                    && row.hasSameKeyAndValue(attributeKey, attributeValue));
  }

  @Override
  public void delete(AlcoholAttribute attribute) {
    rows.removeIf(row -> Objects.equals(row.getId(), attribute.getId()));
  }

  @Override
  public void deleteAllByIdIn(Collection<Long> ids) {
    Set<Long> targets = Set.copyOf(ids);
    rows.removeIf(row -> targets.contains(row.getId()));
  }

  public List<AlcoholAttribute> findAll() {
    return List.copyOf(rows);
  }

  private record KeyGroup(String key, AttributeType type, AttributeDatatype datatype) {}
}
