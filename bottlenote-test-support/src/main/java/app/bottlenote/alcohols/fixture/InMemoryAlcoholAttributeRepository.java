package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
  public boolean existsByAlcoholIdAndAttributeKeyAndAttributeValue(
      Long alcoholId, String attributeKey, String attributeValue) {
    return rows.stream()
        .anyMatch(
            row ->
                Objects.equals(row.getAlcoholId(), alcoholId)
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
}
