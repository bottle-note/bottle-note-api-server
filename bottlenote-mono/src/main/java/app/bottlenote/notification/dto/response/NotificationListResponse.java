package app.bottlenote.notification.dto.response;

import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

/** 서비스 계층 알림 목록 조회 결과. */
public record NotificationListResponse(long totalCount, List<Item> items) {

  public static NotificationListResponse of(long totalCount, List<Item> items) {
    return new NotificationListResponse(totalCount, items);
  }

  /** 알림함 목록 항목. */
  public record Item(
      Long id,
      String title,
      String content,
      NotificationType type,
      NotificationCategory category,
      NotificationStatus status,
      Boolean isRead,
      LocalDateTime createAt) {}
}
