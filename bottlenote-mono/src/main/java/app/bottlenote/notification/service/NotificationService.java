package app.bottlenote.notification.service;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.payload.NotificationMessage;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  PageResponse<NotificationListResponse> getNotifications(
      Long userId, NotificationPageableRequest request);

  long countUnread(Long userId);

  NotificationMarkReadResult markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
