package app.bottlenote.notification.service;

import app.bottlenote.notification.constant.NotificationKind;
import app.bottlenote.notification.domain.NotificationPreferenceRepository;
import app.bottlenote.notification.dto.request.NotificationPreferenceRequest;
import app.bottlenote.notification.dto.response.NotificationPreferenceResponse;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultNotificationPreferenceService implements NotificationPreferenceService {
  private final NotificationPreferenceRepository preferenceRepository;
  private final UserFacade userFacade;

  @Override
  @Transactional(readOnly = true)
  public NotificationPreferenceResponse getPreferences(Long userId) {
    requireUser(userId);
    return loadPreferences(userId);
  }

  @Override
  @Transactional
  public NotificationPreferenceResponse updatePreferences(Long userId, NotificationPreferenceRequest request) {
    requireUser(userId);
    if (request == null || request.settings() == null || request.settings().isEmpty()
        || request.settings().size() > NotificationKind.values().length
        || request.settings().entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
      throw new NotificationException(NotificationExceptionCode.INVALID_NOTIFICATION_PREFERENCE);
    }
    preferenceRepository.update(userId, request.settings());
    return loadPreferences(userId);
  }

  private NotificationPreferenceResponse loadPreferences(Long userId) {
    Map<NotificationKind, Boolean> settings = new EnumMap<>(NotificationKind.class);
    for (NotificationKind kind : NotificationKind.values()) {
      settings.put(kind, true);
    }
    settings.putAll(preferenceRepository.findByUserId(userId));
    return new NotificationPreferenceResponse(settings);
  }

  private void requireUser(Long userId) {
    if (!Boolean.TRUE.equals(userFacade.existsByUserId(userId))) {
      throw new UserException(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND);
    }
  }
}
