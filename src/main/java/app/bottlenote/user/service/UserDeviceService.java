package app.bottlenote.user.service;

import app.bottlenote.user.domain.DeviceTokenRepository;
import app.bottlenote.user.domain.Platform;
import app.bottlenote.user.domain.UserDeviceToken;
import app.bottlenote.user.dto.response.TokenSaveResponse;
import app.external.push.dto.model.TokenMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {
	private final DeviceTokenRepository deviceTokenRepository;

	public TokenSaveResponse saveUserToken(
		final Long userId,
		final String deviceToken,
		final Platform platform
	) {
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

	/*
	public List<String> loadUserTokens(final List<String> userIds) {
		return deviceTokenRepository.findTokensByUserIds(userIds)
			.stream()
			.distinct()
			.filter(item -> userFacade.canSendPushNow(item.getUserId()))
			.map(UserDeviceToken::getDeviceToken)
			.toList();
	}*/
}
