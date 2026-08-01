package app.bottlenote.user.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;

@DomainRepository
public interface UserAgreementRepository {
  void saveAgreements(List<UserAgreement> agreements);
}
