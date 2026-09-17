package app.bottlenote.notification.fixture;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserNotificationSettingRepository implements UserNotificationSettingRepository {
  private record Key(Long userId, NotificationEventAction action) {}
  private final Map<Key, UserNotificationSetting> settings = new HashMap<>();

  @Override
  public Optional<UserNotificationSetting> findByUserIdAndActionCode(Long userId, NotificationEventAction action) {
    return Optional.ofNullable(settings.get(new Key(userId, action)));
  }

  @Override
  public void saveOverride(UserNotificationSetting setting) {
    settings.put(new Key(setting.getUserId(), setting.getActionCode()), setting);
  }

  @Override
  public void deleteByUserIdAndActionCode(Long userId, NotificationEventAction action) {
    settings.remove(new Key(userId, action));
  }
}
