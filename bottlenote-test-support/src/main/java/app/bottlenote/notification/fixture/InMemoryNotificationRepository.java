package app.bottlenote.notification.fixture;

import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import java.time.LocalDateTime;
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
  public List<Notification> findPageByUserId(NotificationListCriteria criteria) {
    return database.stream()
        .filter(notification -> matches(notification, criteria))
        .filter(
            notification ->
                !criteria.hasCursor() || notification.getId() < criteria.cursor())
        .sorted(Comparator.comparing(Notification::getId).reversed())
        .limit(criteria.fetchLimit())
        .toList();
  }

  @Override
  public long countByCriteria(NotificationListCriteria criteria) {
    return database.stream().filter(notification -> matches(notification, criteria)).count();
  }

  @Override
  public long countByUserId(Long userId) {
    return database.stream().filter(notification -> notification.getUserId().equals(userId)).count();
  }

  @Override
  public long countByUserIdAndIsReadFalse(Long userId) {
    return database.stream()
        .filter(notification -> notification.getUserId().equals(userId))
        .filter(notification -> Boolean.FALSE.equals(notification.getIsRead()))
        .count();
  }

  @Override
  public int markAsReadByIdAndUserId(Long id, Long userId, LocalDateTime readAt) {
    Optional<Notification> notification = findByIdAndUserId(id, userId);
    if (notification.isEmpty() || Boolean.TRUE.equals(notification.get().getIsRead())) {
      return 0;
    }
    notification.get().markAsRead(readAt);
    return 1;
  }

  @Override
  public int markAllAsReadByUserId(Long userId, LocalDateTime readAt) {
    int updated = 0;
    for (Notification notification : database) {
      if (notification.getUserId().equals(userId)
          && Boolean.FALSE.equals(notification.getIsRead())) {
        notification.markAsRead(readAt);
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

  private boolean matches(Notification notification, NotificationListCriteria criteria) {
    if (!notification.getUserId().equals(criteria.userId())) {
      return false;
    }
    if (!criteria.types().isEmpty() && !criteria.types().contains(notification.getType())) {
      return false;
    }
    if (!criteria.categories().isEmpty()
        && !criteria.categories().contains(notification.getCategory())) {
      return false;
    }
    if (criteria.readStatus() == NotificationReadStatus.READ
        && !Boolean.TRUE.equals(notification.getIsRead())) {
      return false;
    }
    if (criteria.readStatus() == NotificationReadStatus.UNREAD
        && !Boolean.FALSE.equals(notification.getIsRead())) {
      return false;
    }
    if ((criteria.createdFrom() != null || criteria.createdTo() != null)
        && notification.getCreateAt() == null) {
      return false;
    }
    if (criteria.createdFrom() != null
        && notification.getCreateAt().isBefore(criteria.createdFrom())) {
      return false;
    }
    return criteria.createdTo() == null
        || notification.getCreateAt().isBefore(criteria.createdTo());
  }
}
