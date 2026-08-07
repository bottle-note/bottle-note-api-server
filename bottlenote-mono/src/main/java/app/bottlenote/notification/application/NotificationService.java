package app.bottlenote.notification.application;

import app.bottlenote.notification.data.payload.NotificationMessage;
import app.bottlenote.notification.domain.Notification;
import java.util.List;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  List<Notification> getNotifications(Long userId);

  long countUnread(Long userId);

  void markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
