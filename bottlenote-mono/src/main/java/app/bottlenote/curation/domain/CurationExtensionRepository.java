package app.bottlenote.curation.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface CurationExtensionRepository {

  Optional<CurationExtension> findByCurationId(Long curationId);

  CurationExtension save(CurationExtension curationExtension);

  void deleteByCurationId(Long curationId);
}
