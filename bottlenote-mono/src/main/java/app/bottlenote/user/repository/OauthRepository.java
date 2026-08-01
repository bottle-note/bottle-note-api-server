package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.socialUniqueId = :socialUniqueId")
  Optional<User> findBySocialUniqueIdForUpdate(@Param("socialUniqueId") String socialUniqueId);

  Optional<User> findByNickName(String nickName);

  Optional<User> findByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.email = :email")
  Optional<User> findByEmailForUpdate(@Param("email") String email);

  Optional<User> findByRefreshToken(String refreshToken);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.id = :userId")
  Optional<User> findByIdForUpdate(@Param("userId") Long userId);

  @Query("select u from users  u order by u.id limit 1")
  Optional<User> getFirstUser();

  @Query("select count (u)+1 from users u")
  String getNextNicknameSequence();
}
