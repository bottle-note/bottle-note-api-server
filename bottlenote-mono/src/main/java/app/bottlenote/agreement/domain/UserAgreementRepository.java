package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface UserAgreementRepository {

  UserAgreement save(UserAgreement userAgreement);

  Optional<UserAgreement> findFirstByUserIdAndAgreementTypeOrderByRecordedAtDescIdDesc(
      Long userId, AgreementType agreementType);
}
