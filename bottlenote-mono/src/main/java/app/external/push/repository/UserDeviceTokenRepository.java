package app.external.push.repository;

import app.external.notification.domain.UserDeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

  @Query(
      """
		SELECT udt
		from user_device_token udt
		where udt.userId = :userId
		and udt.deviceToken = :deviceToken
		""")
  Optional<UserDeviceToken> findByUserIdAndDeviceToken(
      @Param("userId") Long userId, @Param("deviceToken") String deviceToken);

  @Query("select t from user_device_token t where t.userId = :userId")
  Optional<UserDeviceToken> findByUserId(@Param("userId") Long userId);

  @Query(
      """
			           SELECT distinct udt
			           from user_device_token udt
			           where udt.userId in :userId
			""")
  List<UserDeviceToken> findTokensByUserIds(@Param("userId") List<String> userId);
}
