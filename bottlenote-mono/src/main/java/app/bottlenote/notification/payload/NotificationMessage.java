package app.bottlenote.notification.payload;

import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;

public record NotificationMessage(
    Long userId, // 알람 대상 사용자 식별자
    NotificationType type, // 알람 종류
    NotificationCategory category, // 알람의 카테고리
    String title, // 알람 내용 category에 따라 다름 참조값이 들어간다.
    String content, // 알람 내용 category에 따라 다름 참조값이 들어간다.
    NotificationSourceType sourceType,
    Long sourceId,
    NotificationAction action
    ) {
  public static NotificationMessage create(
      Long userId,
      NotificationType type,
      NotificationCategory category,
      String title,
      String content) {
    return new NotificationMessage(userId, type, category, title, content, null, null, null);
  }

  public static NotificationMessage reviewReply(
      Long userId, Long reviewId, Long replyId, String title, String content) {
    NotificationAction action = NotificationAction.openReview(reviewId, replyId);
    return new NotificationMessage(
        userId,
        NotificationType.USER,
        NotificationCategory.REVIEW,
        title,
        content,
        NotificationSourceType.REVIEW_REPLY,
        replyId,
        action);
  }
}
