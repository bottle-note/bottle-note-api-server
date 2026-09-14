package app.bottlenote.notification.repository;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import java.util.List;

/** 알림함 커서 페이징 조회. */
public interface CustomNotificationRepository {

  void saveIfAbsent(Notification notification);

  List<Notification> findPageByUserId(NotificationListCriteria criteria);

  /**
   * cursor를 제외하고 목록 필터에 일치하는 알림 수를 집계한다.
   *
   * <p>페이지 이동과 무관한 전체 건수를 응답하기 위해 사용한다.
   */
  long countByCriteria(NotificationListCriteria criteria);

  long countByUserId(Long userId);
}
