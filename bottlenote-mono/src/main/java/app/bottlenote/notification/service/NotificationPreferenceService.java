package app.bottlenote.notification.service;

import app.bottlenote.notification.dto.request.NotificationPreferenceRequest;
import app.bottlenote.notification.dto.response.NotificationPreferenceResponse;

public interface NotificationPreferenceService {
  NotificationPreferenceResponse getPreferences(Long userId);

  NotificationPreferenceResponse updatePreferences(Long userId, NotificationPreferenceRequest request);
}
