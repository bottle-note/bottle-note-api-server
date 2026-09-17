package app.bottlenote.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.notification.constant.NotificationEventAction;
import java.util.Optional;

@DomainRepository
public interface UserNotificationSettingRepository {
  Optional<UserNotificationSetting> findByUserIdAndActionCode(
      Long userId, NotificationEventAction actionCode);

  void saveOverride(UserNotificationSetting setting);

  void deleteByUserIdAndActionCode(Long userId, NotificationEventAction actionCode);
}
