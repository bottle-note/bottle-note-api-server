package app.bottlenote.notification.dto.response;

import app.bottlenote.notification.domain.Notification;
import java.util.List;

/** 서비스 계층 알림 목록 조회 결과. */
public record NotificationListResult(long totalCount, List<Notification> items) {

  public static NotificationListResult of(long totalCount, List<Notification> items) {
    return new NotificationListResult(totalCount, items);
  }
}
