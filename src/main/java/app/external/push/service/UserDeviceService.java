package app.external.push.service;

import app.bottlenote.user.service.UserFacade;
import app.external.push.domain.DeviceTokenRepository;
import app.external.push.domain.Platform;
import app.external.push.domain.UserDeviceToken;
import app.external.push.dto.model.TokenMessage;
import app.external.push.dto.response.TokenSaveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {
	private final DeviceTokenRepository deviceTokenRepository;
	private final UserFacade userDomainSupport;

	public TokenSaveResponse saveUserToken(Long userId, String deviceToken, Platform platform) {
		deviceTokenRepository.findByUserIdAndDeviceToken(userId, deviceToken)
			.ifPresentOrElse(
				userDeviceToken -> {
					userDeviceToken.updateModifiedAt();
					deviceTokenRepository.save(userDeviceToken);
				},
				() -> deviceTokenRepository.save(UserDeviceToken.builder()
					.userId(userId)
					.deviceToken(deviceToken)
					.platform(platform)
					.build())
			);
		return TokenSaveResponse.of(deviceToken, platform, TokenMessage.DEVICE_TOKEN_SAVED);
	}
}
