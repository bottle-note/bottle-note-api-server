package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCurationExtensionRepository implements CurationExtensionRepository {

  private final Map<Long, CurationExtension> database = new HashMap<>();

  @Override
  public Optional<CurationExtension> findByCurationId(Long curationId) {
    return Optional.ofNullable(database.get(curationId));
  }

  @Override
  public List<CurationExtension> findAllByCurationIdIn(Collection<Long> curationIds) {
    return database.values().stream()
        .filter(extension -> curationIds.contains(extension.getCurationId()))
        .toList();
  }

  @Override
  public CurationExtension save(CurationExtension curationExtension) {
    database.put(curationExtension.getCurationId(), curationExtension);
    return curationExtension;
  }

  @Override
  public void deleteByCurationId(Long curationId) {
    database.remove(curationId);
  }
}
