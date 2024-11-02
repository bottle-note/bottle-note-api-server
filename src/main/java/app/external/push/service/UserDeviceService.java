package app.external.push.service;

import app.bottlenote.user.service.domain.UserDomainSupport;
import app.external.push.domain.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {
	private final DeviceTokenRepository deviceTokenRepository;
	private final UserDomainSupport userDomainSupport;

	public Object saveUserToken() {
		return null;
	}
}
