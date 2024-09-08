package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import feign.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface OauthRepository extends CrudRepository<User, Long> {

	@Query(value = "SELECT * FROM users u WHERE u.email = :email AND JSON_CONTAINS(u.social_type, JSON_QUOTE(:socialType), '$')", nativeQuery = true)
	Optional<User> findByEmailAndSocialType(@Param("email") String email, @Param("socialType") String socialType);

	Optional<User> findByNickName(String nickName);

	Optional<User> findByEmail(String email);

	Optional<User> findByRefreshToken(String refreshToken);
}
