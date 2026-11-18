package app.bottlenote.curation.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface CurationExtensionRepository {

  Optional<CurationExtension> findByCurationId(Long curationId);

  List<CurationExtension> findAllByCurationIdIn(Collection<Long> curationIds);

  List<CurationExtension> findAllBySpecIdIn(Collection<Long> specIds);

  CurationExtension save(CurationExtension curationExtension);

  void deleteByCurationId(Long curationId);
}
