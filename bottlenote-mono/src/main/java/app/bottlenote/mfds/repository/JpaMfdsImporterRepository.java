package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaMfdsImporterRepository
    extends MfdsImporterRepository,
        JpaRepository<MfdsImporter, Long>,
        CustomMfdsImporterRepository {

  @Override
  @Query("select i from mfds_importer i where i.id in :ids")
  List<MfdsImporter> findAllByIdIn(@Param("ids") Collection<Long> ids);

  @Override
  Optional<MfdsImporter> findByOfficialBusinessCode(String officialBusinessCode);
}
