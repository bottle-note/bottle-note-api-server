package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingKey;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholAttributeMappingRepository
    implements AlcoholAttributeMappingRepository {
  private final Map<Long, AlcoholAttributeMapping> rows = new LinkedHashMap<>();
  private final AlcoholQueryRepository alcohols;
  private long sequence;

  public InMemoryAlcoholAttributeMappingRepository(AlcoholQueryRepository alcohols) {
    this.alcohols = alcohols;
  }

  @Override
  public AlcoholAttributeMapping save(AlcoholAttributeMapping mapping) {
    if (rows.values().stream()
        .anyMatch(
            m ->
                !Objects.equals(m.getId(), mapping.getId())
                    && m.getAlcoholId().equals(mapping.getAlcoholId())
                    && m.getAttributeId().equals(mapping.getAttributeId())))
      throw new IllegalStateException("duplicate mapping");
    if (mapping.getId() == null) ReflectionTestUtils.setField(mapping, "id", ++sequence);
    rows.put(mapping.getId(), mapping);
    return mapping;
  }

  @Override
  public List<AlcoholAttributeMappingKey> findExistingKeys(
      Collection<Long> alcoholIds, Collection<Long> attributeIds) {
    return rows.values().stream()
        .filter(
            m -> alcoholIds.contains(m.getAlcoholId()) && attributeIds.contains(m.getAttributeId()))
        .map(m -> new AlcoholAttributeMappingKey(m.getAlcoholId(), m.getAttributeId()))
        .toList();
  }

  @Override
  public Optional<AlcoholAttributeMapping> findById(Long id) {
    return Optional.ofNullable(rows.get(id));
  }

  @Override
  public List<AlcoholAttributeMapping> findAllByAlcoholIdIn(Collection<Long> ids) {
    return rows.values().stream().filter(m -> ids.contains(m.getAlcoholId())).toList();
  }

  @Override
  public OffsetPage<AlcoholAttributeMapping> search(
      AdminAlcoholAttributeMappingSearchRequest request) {
    var matched =
        rows.values().stream()
            .filter(
                m -> request.alcoholId() == null || request.alcoholId().equals(m.getAlcoholId()))
            .filter(
                m ->
                    request.attributeId() == null
                        || request.attributeId().equals(m.getAttributeId()))
            .toList();
    return new OffsetPage<>(
        matched.stream()
            .skip((long) request.page() * request.size())
            .limit(request.size())
            .toList(),
        matched.size());
  }

  @Override
  public List<Alcohol> lockAlcohols(Collection<Long> ids) {
    return alcohols.findAllByIdIn(List.copyOf(ids));
  }

  @Override
  public void delete(AlcoholAttributeMapping mapping) {
    rows.remove(mapping.getId());
  }

  @Override
  public void deleteAllByAttributeIdIn(Collection<Long> ids) {
    rows.values().removeIf(m -> ids.contains(m.getAttributeId()));
  }

  public List<AlcoholAttributeMapping> findAll() {
    return List.copyOf(rows.values());
  }
}
