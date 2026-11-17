package app.bottlenote.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.notification.constant.NotificationEventAction;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface UserNotificationSettingRepository {
  Optional<UserNotificationSetting> findByUserIdAndActionCode(
      Long userId, NotificationEventAction actionCode);

  List<UserNotificationSetting> findAllByUserId(Long userId);

  UserNotificationSetting insert(UserNotificationSetting setting);

  void deleteByUserIdAndActionCode(Long userId, NotificationEventAction actionCode);
}
