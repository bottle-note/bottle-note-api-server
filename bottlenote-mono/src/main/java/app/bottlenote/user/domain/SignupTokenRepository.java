package app.bottlenote.user.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;
import java.util.UUID;

@DomainRepository
public interface SignupTokenRepository {
  SignupToken saveToken(SignupToken signupToken);

  Optional<SignupToken> findByTokenIdForUpdate(UUID tokenId);
}
