package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLinkRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaMfdsImporterRcnoLinkRepository
    extends MfdsImporterRcnoLinkRepository, JpaRepository<MfdsImporterRcnoLink, String> {

  @Override
  Optional<MfdsImporterRcnoLink> findByRcno(String rcno);

  @Override
  List<MfdsImporterRcnoLink> findAllByImporterId(Long importerId);

  @Override
  long countByImporterId(Long importerId);
}
