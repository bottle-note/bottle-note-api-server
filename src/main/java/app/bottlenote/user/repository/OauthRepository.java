package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface OauthRepository extends CrudRepository<User, Long> {

	Optional<User> findByEmailAndSocialType(String email, SocialType socialType);

	Optional<User> findByNickName(String nickName);

	Optional<User> findByEmail(String email);

	Optional<User> findByRefreshToken(String refreshToken);
}
