package app.external.notification.data.payload;

import app.external.notification.domain.constant.NotificationCategory;
import app.external.notification.domain.constant.NotificationType;

public record NotificationMessage(
    Long userId, // 알람 대상 사용자 식별자
    NotificationType type, // 알람 종류
    NotificationCategory category, // 알람의 카테고리
    String title, // 알람 내용 category에 따라 다름 참조값이 들어간다.
    String content // 알람 내용 category에 따라 다름 참조값이 들어간다.
    ) {
  public static NotificationMessage create(
      Long userId,
      NotificationType type,
      NotificationCategory category,
      String title,
      String content) {
    return new NotificationMessage(userId, type, category, title, content);
  }
}
