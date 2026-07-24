package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationExtensionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaCurationExtensionRepository
    extends CurationExtensionRepository, JpaRepository<CurationExtension, Long> {

  Optional<CurationExtension> findByCurationId(Long curationId);

  List<CurationExtension> findAllByCurationIdIn(Collection<Long> curationIds);

  void deleteByCurationId(Long curationId);
}
