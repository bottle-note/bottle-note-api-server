package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaMfdsDeclarationRepository
    extends MfdsDeclarationRepository,
        JpaRepository<MfdsDeclaration, Long>,
        CustomMfdsDeclarationRepository {

  @Override
  Optional<MfdsDeclaration> findByRcno(String rcno);
}
