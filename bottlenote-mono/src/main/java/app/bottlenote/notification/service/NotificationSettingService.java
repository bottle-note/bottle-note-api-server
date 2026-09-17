package app.bottlenote.notification.service;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
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

  private final TransactionTemplate settingTransaction;

  public NotificationSettingService(
      UserNotificationSettingRepository repository, PlatformTransactionManager transactionManager) {
    this.repository = repository;
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

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void changeSetting(Long userId, NotificationEventAction action, boolean enabled) {
    validate(userId, action);
    // 동시 INSERT의 중복 키만 롤백 완료 후 정상 처리한다.
    try {
      settingTransaction.executeWithoutResult(
          status -> {
            if (enabled == action.isDefaultEnabled()) {
              repository.deleteByUserIdAndActionCode(userId, action);
            } else if (repository.findByUserIdAndActionCode(userId, action).isEmpty()) {
              repository.insert(new UserNotificationSetting(userId, action, enabled));
            }
          });
    } catch (NotificationException exception) {
      if (exception.getExceptionCode() != NotificationExceptionCode.DUPLICATE_NOTIFICATION_KEY) {
        throw exception;
      }
      log.debug("이미 저장된 알림 설정 - userId: {}, action: {}", userId, action);
    }
  }

  private void validate(Long userId, NotificationEventAction action) {
    Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
    Objects.requireNonNull(action, "알림 발생 액션은 필수입니다.");
    if (userId <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
    }
  }
}
