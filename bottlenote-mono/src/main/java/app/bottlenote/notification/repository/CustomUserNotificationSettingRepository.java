package app.bottlenote.notification.repository;

import app.bottlenote.notification.domain.UserNotificationSetting;

public interface CustomUserNotificationSettingRepository {
  void saveOverride(UserNotificationSetting setting);
}
