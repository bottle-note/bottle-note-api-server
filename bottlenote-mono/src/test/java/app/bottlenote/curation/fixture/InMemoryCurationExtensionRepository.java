package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCurationExtensionRepository implements CurationExtensionRepository {

  private final Map<Long, CurationExtension> database = new HashMap<>();

  @Override
  public Optional<CurationExtension> findByCurationId(Long curationId) {
    return Optional.ofNullable(database.get(curationId));
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
