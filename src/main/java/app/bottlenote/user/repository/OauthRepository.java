package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OauthRepository extends CrudRepository<User, Long> {

	Optional<User> findByEmailAndSocialType(String email, SocialType socialType);

	Optional<User> findByNickName(String nickName);

	Optional<User> findByEmail(String email);

	Optional<User> findByRefreshToken(String refreshToken);

	@Modifying
	@Query("UPDATE users u SET u.refreshToken = :refreshToken WHERE u.id = :userId")
	void updateUserByRefreshToken(@Param("refreshToken") String refreshToken,
		@Param("userId") Long userId);
}
