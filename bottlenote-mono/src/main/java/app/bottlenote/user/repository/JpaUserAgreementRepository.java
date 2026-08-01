package app.bottlenote.user.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.user.domain.UserAgreement;
import app.bottlenote.user.domain.UserAgreementRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaUserAgreementRepository
    extends UserAgreementRepository, JpaRepository<UserAgreement, Long> {

  @Override
  default void saveAgreements(List<UserAgreement> agreements) {
    saveAll(agreements);
  }
}
