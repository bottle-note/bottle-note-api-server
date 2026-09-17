package app.bottlenote.notification.fixture;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserNotificationSettingRepository implements UserNotificationSettingRepository {
  private record Key(Long userId, NotificationEventAction action) {}

  private final Map<Key, UserNotificationSetting> settings = new HashMap<>();

  @Override
  public Optional<UserNotificationSetting> findByUserIdAndActionCode(
      Long userId, NotificationEventAction action) {
    return Optional.ofNullable(settings.get(new Key(userId, action)));
  }

  @Override
  public UserNotificationSetting insert(UserNotificationSetting setting) {
    Key key = new Key(setting.getUserId(), setting.getActionCode());
    if (settings.containsKey(key)) {
      throw new NotificationException(NotificationExceptionCode.DUPLICATE_NOTIFICATION_KEY);
    }
    settings.put(key, setting);
    return setting;
  }

  @Override
  public void deleteByUserIdAndActionCode(Long userId, NotificationEventAction action) {
    settings.remove(new Key(userId, action));
  }
}
