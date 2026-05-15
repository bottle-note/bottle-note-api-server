package app.bottlenote.curation.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.domain.CurationSpecRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaCurationSpecRepository
    extends CurationSpecRepository, JpaRepository<CurationSpec, Long> {

  Optional<CurationSpec> findByCode(String code);

  List<CurationSpec> findAllByIsActiveTrueOrderByIdAsc();

  List<CurationSpec> findAllByIdIn(Collection<Long> ids);

  boolean existsByCode(String code);
}
