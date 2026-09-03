package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaMfdsImporterRepository
    extends MfdsImporterRepository,
        JpaRepository<MfdsImporter, Long>,
        CustomMfdsImporterRepository {

  @Override
  List<MfdsImporter> findAllByIdInAndAdminStatus(
      Collection<Long> ids, MfdsImporterAdminStatus adminStatus);

  @Override
  Optional<MfdsImporter> findByOfficialBusinessCode(String officialBusinessCode);
}
