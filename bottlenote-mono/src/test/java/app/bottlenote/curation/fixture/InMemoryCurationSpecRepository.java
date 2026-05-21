package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCurationSpecRepository implements CurationSpecRepository {

  private final Map<Long, CurationSpec> database = new HashMap<>();

  @Override
  public Optional<CurationSpec> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<CurationSpec> findByCode(String code) {
    return database.values().stream().filter(spec -> spec.getCode().equals(code)).findFirst();
  }

  @Override
  public List<CurationSpec> findAllByIsActiveTrueOrderByIdAsc() {
    return database.values().stream()
        .filter(spec -> Boolean.TRUE.equals(spec.getIsActive()))
        .sorted(java.util.Comparator.comparing(CurationSpec::getId))
        .toList();
  }

  @Override
  public List<CurationSpec> findAllByIdIn(Collection<Long> ids) {
    return database.values().stream().filter(spec -> ids.contains(spec.getId())).toList();
  }

  @Override
  public boolean existsByCode(String code) {
    return findByCode(code).isPresent();
  }

  @Override
  public CurationSpec save(CurationSpec curationSpec) {
    Long id = curationSpec.getId();
    if (id == null) {
      id = database.size() + 1L;
      ReflectionTestUtils.setField(curationSpec, "id", id);
    }
    database.put(id, curationSpec);
    return curationSpec;
  }
}
