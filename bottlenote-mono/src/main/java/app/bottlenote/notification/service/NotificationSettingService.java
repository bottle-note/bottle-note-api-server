package app.bottlenote.notification.service;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {
  private final UserNotificationSettingRepository repository;

  @Transactional(readOnly = true)
  public boolean isEnabled(Long userId, NotificationEventAction action) {
    validate(userId, action);
    return repository
        .findByUserIdAndActionCode(userId, action)
        .map(UserNotificationSetting::isEnabled)
        .orElse(action.isDefaultEnabled());
  }

  @Transactional
  public void changeSetting(Long userId, NotificationEventAction action, boolean enabled) {
    validate(userId, action);
    if (enabled == action.isDefaultEnabled()) {
      repository.deleteByUserIdAndActionCode(userId, action);
      return;
    }
    repository.saveOverride(new UserNotificationSetting(userId, action, enabled));
  }

  private void validate(Long userId, NotificationEventAction action) {
    Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
    Objects.requireNonNull(action, "알림 발생 액션은 필수입니다.");
    if (userId <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
    }
  }
}
