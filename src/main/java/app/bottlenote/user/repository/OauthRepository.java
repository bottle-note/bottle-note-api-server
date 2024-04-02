package app.bottlenote.user.repository;

import app.bottlenote.user.domain.Users;
import app.bottlenote.user.constant.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OauthRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByEmailAndSocialType(String email, SocialType socialType);
}
