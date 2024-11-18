package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import feign.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OauthRepository extends CrudRepository<User, Long> {

	@Query(value = "SELECT * FROM users u WHERE u.email = :email AND JSON_CONTAINS(u.social_type, JSON_QUOTE(:socialType), '$')", nativeQuery = true)
	Optional<User> findByEmailAndSocialType(@Param("email") String email, @Param("socialType") String socialType);

	Optional<User> findByNickName(String nickName);

	Optional<User> findByEmail(String email);

	Optional<User> findByRefreshToken(String refreshToken);

	@Query("""
		SELECT u FROM users u
		WHERE u.role = 'ROLE_GUEST'
		order by u.id
		limit 1
		""")
	Optional<User> loadGuestUser();

	@Query("select max(u.id)+1 from users u")
	Long getNextNicknameSequence();
}
