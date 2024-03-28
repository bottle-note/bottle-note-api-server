package app.bottlenote.oauth.repository;

import app.bottlenote.user.domain.Users;
import app.bottlenote.oauth.constant.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OauthLoginRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByEmailAndSocialType(String email, SocialType socialType);
}
