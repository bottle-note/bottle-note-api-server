package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

/** 사용자 약관 동의 이력의 저장과 조회를 정의한다. */
@DomainRepository
public interface UserAgreementRepository {

  UserAgreement save(UserAgreement userAgreement);

  Optional<UserAgreement> findFirstByUserIdAndAgreementTypeOrderByIdDesc(
      Long userId, AgreementType agreementType);
}
