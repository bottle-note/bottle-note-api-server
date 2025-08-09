package app.external.push.application;

import app.external.notification.domain.UserDeviceToken;
import app.external.notification.domain.constant.Platform;
import app.external.push.data.payload.TokenMessage;
import app.external.push.data.response.TokenSaveResponse;
import app.external.push.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {
  private final UserDeviceTokenRepository deviceTokenRepository;

  public TokenSaveResponse saveUserToken(
      final Long userId, final String deviceToken, final Platform platform) {
    deviceTokenRepository
        .findByUserIdAndDeviceToken(userId, deviceToken)
        .ifPresentOrElse(
            userDeviceToken -> {
              userDeviceToken.updateModifiedAt();
              deviceTokenRepository.save(userDeviceToken);
            },
            () ->
                deviceTokenRepository.save(
                    UserDeviceToken.builder()
                        .userId(userId)
                        .deviceToken(deviceToken)
                        .platform(platform)
                        .build()));
    return TokenSaveResponse.of(deviceToken, platform, TokenMessage.DEVICE_TOKEN_SAVED);
  }
}
