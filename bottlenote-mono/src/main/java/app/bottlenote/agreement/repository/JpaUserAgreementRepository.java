package app.bottlenote.agreement.repository;

import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaUserAgreementRepository
    extends UserAgreementRepository, JpaRepository<UserAgreement, Long> {}
