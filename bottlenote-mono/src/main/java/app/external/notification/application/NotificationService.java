package app.external.notification.application;

import app.external.notification.data.payload.NotificationMessage;
import app.external.notification.domain.Notification;
import java.util.List;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  List<Notification> getNotifications(Long userId);

  long countUnread(Long userId);

  void markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
