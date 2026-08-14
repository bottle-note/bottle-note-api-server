package app.bottlenote.notification.service;

import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.payload.NotificationMessage;

public interface NotificationService {

  void sendNotification(NotificationMessage message);

  PageResponse<NotificationListResponse> getNotifications(
      Long userId, NotificationPageableRequest request);

  long countUnread(Long userId);

  /**
   * 사용자 소유 알림을 멱등하게 읽음 처리한다.
   *
   * <p>최초 읽음 시각과 처리 후 미읽음 개수를 함께 반환한다.
   */
  NotificationMarkReadResult markAsRead(Long userId, Long notificationId);

  int markAllAsRead(Long userId);
}
