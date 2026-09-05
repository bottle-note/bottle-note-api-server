package app.bottlenote.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.notification.constant.NotificationKind;
import java.util.Map;

@DomainRepository
public interface NotificationPreferenceRepository {
  Map<NotificationKind, Boolean> findByUserId(Long userId);

  void update(Long userId, Map<NotificationKind, Boolean> settings);
}
