package app.bottlenote.notification.service;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.payload.NotificationMessage;
import java.util.List;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  List<Notification> getNotifications(Long userId);

  long countUnread(Long userId);

  void markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
