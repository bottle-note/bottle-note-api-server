package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import feign.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface OauthRepository extends CrudRepository<User, Long> {

  @Query(
      value =
          """
			SELECT u.*
			FROM users u
			WHERE u.email = :email
			   AND JSON_CONTAINS(u.social_type, JSON_QUOTE(:socialType), '$')
			""",
      nativeQuery = true)
  Optional<User> findByEmailAndSocialType(
      @Param("email") String email, @Param("socialType") String socialType);

  Optional<User> findBySocialUniqueId(String socialUniqueId);

  Optional<User> findByNickName(String nickName);

  Optional<User> findByEmail(String email);

  Optional<User> findByRefreshToken(String refreshToken);

  @Query("select u from users  u order by u.id limit 1")
  Optional<User> getFirstUser();

  @Query(
      """
			SELECT u
			FROM users u
			WHERE u.role = 'ROLE_GUEST'
			ORDER BY u.id
			LIMIT 1
			""")
  Optional<User> loadGuestUser();

  @Query("select count (u)+1 from users u")
  String getNextNicknameSequence();
}
