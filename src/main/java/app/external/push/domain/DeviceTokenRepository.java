package app.external.push.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

	@Query("""
		SELECT udt
		from user_device_token udt
		where udt.userId = :userId
		and udt.deviceToken = :deviceToken
		""")
	Optional<UserDeviceToken> findByUserIdAndDeviceToken(Long userId, String deviceToken);

	@Query("select t from user_device_token t where t.userId = :userId")
	Optional<UserDeviceToken> findByUserId(@Param("userId") Long userId);
}
