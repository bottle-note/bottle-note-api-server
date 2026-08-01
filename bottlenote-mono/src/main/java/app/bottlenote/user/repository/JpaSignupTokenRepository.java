package app.bottlenote.user.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.user.domain.SignupToken;
import app.bottlenote.user.domain.SignupTokenRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaSignupTokenRepository
    extends SignupTokenRepository, JpaRepository<SignupToken, Long> {

  @Override
  default SignupToken saveToken(SignupToken signupToken) {
    return save(signupToken);
  }

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select st from signup_tokens st where st.tokenId = :tokenId")
  Optional<SignupToken> findByTokenIdForUpdate(@Param("tokenId") UUID tokenId);
}
