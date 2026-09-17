package app.bottlenote.notification.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.domain.UserNotificationSettingRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaUserNotificationSettingRepository
    extends UserNotificationSettingRepository,
        JpaRepository<UserNotificationSetting, Long>,
        CustomUserNotificationSettingRepository {}
