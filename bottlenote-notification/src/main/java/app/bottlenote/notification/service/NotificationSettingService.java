package app.bottlenote.notification.service;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import app.bottlenote.user.facade.UserFacade;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class NotificationSettingService {
  private final UserNotificationSettingRepository repository;
  private final UserFacade userFacade;

  private final TransactionTemplate settingTransaction;

  public NotificationSettingService(
      UserNotificationSettingRepository repository,
      UserFacade userFacade,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.userFacade = userFacade;
    this.settingTransaction = new TransactionTemplate(transactionManager);
    this.settingTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional(readOnly = true)
  public boolean isEnabled(Long userId, NotificationEventAction action) {
    validate(userId, action);
    return repository
        .findByUserIdAndActionCode(userId, action)
        .map(UserNotificationSetting::isEnabled)
        .orElse(action.isDefaultEnabled());
  }

  /** 모든 발생 액션의 유효 수신 여부를 enum 선언 순서로 반환한다. */
  @Transactional(readOnly = true)
  public Map<NotificationEventAction, Boolean> getSettings(Long userId) {
    validateUserId(userId);
    Map<NotificationEventAction, Boolean> settings = new EnumMap<>(NotificationEventAction.class);
    for (NotificationEventAction action : NotificationEventAction.values()) {
      settings.put(action, action.isDefaultEnabled());
    }
    repository
        .findAllByUserId(userId)
        .forEach(setting -> settings.put(setting.getActionCode(), setting.isEnabled()));
    return settings;
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void changeSetting(Long userId, NotificationEventAction action, boolean enabled) {
    validate(userId, action);
    changeSettings(userId, Map.of(action, enabled));
  }

  /** 여러 발생 액션의 수신 여부를 한 트랜잭션으로 모두 반영하거나 모두 반영하지 않는다. 동시 요청은 나중에 커밋된 요청이 전부 반영된다. */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void changeSettings(Long userId, Map<NotificationEventAction, Boolean> changes) {
    validateUserId(userId);
    Objects.requireNonNull(changes, "변경할 알림 설정은 필수입니다.");
    if (changes.isEmpty()) {
      throw new IllegalArgumentException("변경할 알림 설정은 최소 1개 이상이어야 합니다.");
    }
    changes.forEach((action, enabled) -> validate(userId, action));
    settingTransaction.executeWithoutResult(
        status -> {
          // 사용자 행 잠금으로 같은 사용자의 일괄 변경을 직렬화해 요청 간 결과가 섞이지 않게 한다.
          userFacade.lockUserForUpdate(userId);
          apply(userId, changes);
        });
  }

  private void apply(Long userId, Map<NotificationEventAction, Boolean> changes) {
    Map<NotificationEventAction, UserNotificationSetting> stored =
        new EnumMap<>(NotificationEventAction.class);
    repository
        .findAllByUserId(userId)
        .forEach(setting -> stored.put(setting.getActionCode(), setting));
    changes.forEach(
        (action, enabled) -> {
          Objects.requireNonNull(enabled, "알림 수신 여부는 필수입니다.");
          if (enabled == action.isDefaultEnabled()) {
            if (stored.containsKey(action)) {
              repository.deleteByUserIdAndActionCode(userId, action);
            }
          } else if (!stored.containsKey(action)) {
            repository.insert(new UserNotificationSetting(userId, action, enabled));
          }
        });
  }

  private void validate(Long userId, NotificationEventAction action) {
    validateUserId(userId);
    Objects.requireNonNull(action, "알림 발생 액션은 필수입니다.");
  }

  private void validateUserId(Long userId) {
    Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
    if (userId <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
    }
  }
}
