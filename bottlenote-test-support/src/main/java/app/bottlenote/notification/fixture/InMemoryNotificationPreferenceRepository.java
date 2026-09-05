package app.bottlenote.notification.fixture;

import app.bottlenote.notification.constant.NotificationKind;
import app.bottlenote.notification.domain.NotificationPreferenceRepository;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class InMemoryNotificationPreferenceRepository implements NotificationPreferenceRepository {
  private final Map<Long, Map<NotificationKind, Boolean>> database = new HashMap<>();

  @Override
  public synchronized Map<NotificationKind, Boolean> findByUserId(Long userId) {
    return Map.copyOf(database.getOrDefault(userId, Map.of()));
  }

  @Override
  public synchronized void update(Long userId, Map<NotificationKind, Boolean> settings) {
    database.computeIfAbsent(userId, ignored -> new EnumMap<>(NotificationKind.class)).putAll(settings);
  }
}
