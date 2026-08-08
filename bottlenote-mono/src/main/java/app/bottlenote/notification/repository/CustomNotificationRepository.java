package app.bottlenote.notification.repository;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import java.util.List;

/** 알림함 커서 페이징 조회. */
public interface CustomNotificationRepository {

  List<Notification> findPageByUserId(NotificationListCriteria criteria);

  long countByUserId(Long userId);
}
