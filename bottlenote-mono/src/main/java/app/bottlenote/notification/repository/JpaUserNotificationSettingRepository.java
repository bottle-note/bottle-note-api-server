package app.bottlenote.notification.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaUserNotificationSettingRepository
    extends UserNotificationSettingRepository, JpaRepository<UserNotificationSetting, Long> {
  @Override
  default UserNotificationSetting insert(UserNotificationSetting setting) {
    try {
      return saveAndFlush(setting);
    } catch (DataIntegrityViolationException exception) {
      if (NotificationConstraintViolation.matches(
          exception, "uk_user_notification_settings_user_action")) {
        throw new NotificationException(
            NotificationExceptionCode.DUPLICATE_NOTIFICATION_KEY, exception);
      }
      throw exception;
    }
  }

  @Override
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "delete from user_notification_setting s where s.userId = :userId and s.actionCode = :action")
  void deleteByUserIdAndActionCode(
      @Param("userId") Long userId, @Param("action") NotificationEventAction action);
}
