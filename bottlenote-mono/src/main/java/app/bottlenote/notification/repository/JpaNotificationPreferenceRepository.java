package app.bottlenote.notification.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.notification.constant.NotificationKind;
import app.bottlenote.notification.domain.NotificationPreferenceRepository;
import jakarta.persistence.EntityManager;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class JpaNotificationPreferenceRepository implements NotificationPreferenceRepository {
  private final EntityManager entityManager;

  @Override
  public Map<NotificationKind, Boolean> findByUserId(Long userId) {
    Map<NotificationKind, Boolean> settings = new EnumMap<>(NotificationKind.class);
    for (Object row : entityManager.createNativeQuery(
        "SELECT kind, enabled FROM notification_preferences WHERE user_id = :userId")
        .setParameter("userId", userId).getResultList()) {
      Object[] values = (Object[]) row;
      for (NotificationKind kind : NotificationKind.values()) {
        if (kind.name().equals(values[0])) {
          Object enabled = values[1];
          settings.put(kind, enabled instanceof Boolean value ? value : ((Number) enabled).intValue() != 0);
        }
      }
    }
    return settings;
  }

  @Override
  public void update(Long userId, Map<NotificationKind, Boolean> settings) {
    // 유형 순서를 고정하여 동시에 여러 설정을 바꿀 때 잠금 순서가 뒤집히지 않게 한다.
    for (NotificationKind kind : NotificationKind.values()) {
      if (settings.containsKey(kind)) {
        entityManager.createNativeQuery("""
            INSERT INTO notification_preferences (user_id, kind, enabled)
            VALUES (:userId, :kind, :enabled)
            ON DUPLICATE KEY UPDATE enabled = :enabled
            """)
            .setParameter("userId", userId)
            .setParameter("kind", kind.name())
            .setParameter("enabled", settings.get(kind))
            .executeUpdate();
      }
    }
  }
}
