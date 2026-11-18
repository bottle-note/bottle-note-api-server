package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
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
  List<MfdsImporter> findAllByIdInAndAdminStatus(
      Collection<Long> ids, MfdsImporterAdminStatus adminStatus);

  @Override
  @Query("select i from mfds_importer i where i.id = :id and i.adminStatus = 'ACTIVE'")
  Optional<MfdsImporter> findActiveById(@Param("id") Long id);

  @Override
  Optional<MfdsImporter> findByOfficialBusinessCode(String officialBusinessCode);
}
