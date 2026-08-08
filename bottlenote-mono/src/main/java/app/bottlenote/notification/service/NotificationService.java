package app.bottlenote.notification.service;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResult;
import app.bottlenote.notification.payload.NotificationMessage;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  PageResponse<NotificationListResult> getNotifications(
      Long userId, NotificationPageableRequest request);

  long countUnread(Long userId);

  void markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
