package app.bottlenote.notification.repository;

import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.notification.domain.UserNotificationSetting;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;

@RequiredArgsConstructor
public class CustomUserNotificationSettingRepositoryImpl
    implements CustomUserNotificationSettingRepository {
  private final EntityManager entityManager;
  private final AuditorAware<AuditPrincipal> auditorAware;

  @Override
  public void saveOverride(UserNotificationSetting setting) {
    AuditPrincipal principal = auditorAware.getCurrentAuditor().orElse(null);
    entityManager
        .createNativeQuery(
            """
      INSERT INTO user_notification_settings
        (user_id, action_code, enabled, create_at, last_modify_at,
         create_principal_id, create_principal_type, create_principal_email,
         last_modify_principal_id, last_modify_principal_type, last_modify_principal_email)
      VALUES (:userId, :actionCode, :enabled, :now, :now,
         :principalId, :principalType, :principalEmail,
         :principalId, :principalType, :principalEmail)
      ON DUPLICATE KEY UPDATE enabled = :enabled, last_modify_at = :now,
         last_modify_principal_id = :principalId, last_modify_principal_type = :principalType,
         last_modify_principal_email = :principalEmail
      """)
        .setParameter("userId", setting.getUserId())
        .setParameter("actionCode", setting.getActionCode().name())
        .setParameter("enabled", setting.isEnabled())
        .setParameter("now", LocalDateTime.now())
        .setParameter("principalId", principal == null ? null : principal.getId())
        .setParameter(
            "principalType",
            principal == null || principal.getType() == null ? null : principal.getType().name())
        .setParameter("principalEmail", principal == null ? null : principal.getEmail())
        .executeUpdate();
  }
}
