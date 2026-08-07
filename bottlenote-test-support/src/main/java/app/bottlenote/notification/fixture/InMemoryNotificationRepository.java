package app.bottlenote.notification.fixture;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.constant.NotificationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/** 알림 도메인 포트의 인메모리 구현체. unit 테스트에서 사용한다. */
public class InMemoryNotificationRepository implements NotificationRepository {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final List<Notification> database = new ArrayList<>();

  @Override
  public Notification save(Notification notification) {
    Objects.requireNonNull(notification, "notification은 null일 수 없습니다.");
    if (notification.getId() == null) {
      ReflectionTestUtils.setField(notification, "id", idGenerator.getAndIncrement());
      database.add(notification);
      return notification;
    }
    database.removeIf(existing -> existing.getId().equals(notification.getId()));
    database.add(notification);
    return notification;
  }

  @Override
  public Optional<Notification> findById(Long id) {
    return database.stream().filter(notification -> notification.getId().equals(id)).findFirst();
  }

  @Override
  public Optional<Notification> findByIdAndUserId(Long id, Long userId) {
    return database.stream()
        .filter(notification -> notification.getId().equals(id))
        .filter(notification -> notification.getUserId().equals(userId))
        .findFirst();
  }

  @Override
  public List<Notification> findAllByUserIdOrderByIdDesc(Long userId) {
    return database.stream()
        .filter(notification -> notification.getUserId().equals(userId))
        .sorted(Comparator.comparing(Notification::getId).reversed())
        .toList();
  }

  @Override
  public long countByUserIdAndIsReadFalse(Long userId) {
    return database.stream()
        .filter(notification -> notification.getUserId().equals(userId))
        .filter(notification -> Boolean.FALSE.equals(notification.getIsRead()))
        .count();
  }

  @Override
  public int markAllAsReadByUserId(Long userId) {
    int updated = 0;
    for (Notification notification : database) {
      if (notification.getUserId().equals(userId) && Boolean.FALSE.equals(notification.getIsRead())) {
        notification.markAsRead();
        updated++;
      }
    }
    return updated;
  }

  public List<Notification> findAll() {
    return List.copyOf(database);
  }

  public void clear() {
    database.clear();
    idGenerator.set(1L);
  }

  /** 테스트에서 읽음 상태를 직접 시드할 때 사용한다. */
  public Notification saveUnread(Notification notification) {
    ReflectionTestUtils.setField(notification, "isRead", false);
    ReflectionTestUtils.setField(notification, "status", NotificationStatus.PENDING);
    return save(notification);
  }
}
