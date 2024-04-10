package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmailAndSocialType(String email, SocialType socialType);

	Optional<User> findByNickName(String nickName);
}
