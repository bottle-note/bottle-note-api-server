package app.bottlenote.agreement.repository;

import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자 약관 동의 이력을 관리하는 JPA 저장소다. */
@JpaRepositoryImpl
public interface JpaUserAgreementRepository
    extends UserAgreementRepository, JpaRepository<UserAgreement, Long> {}
