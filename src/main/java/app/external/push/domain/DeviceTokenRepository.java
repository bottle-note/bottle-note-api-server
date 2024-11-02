package app.external.push.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {
}
