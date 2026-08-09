package app.bottlenote.notification.payload;

import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;

/**
 * 알림 저장 이벤트가 전달하는 채널 독립 메시지다.
 *
 * <p>원본 이벤트 식별자와 의미 기반 Action을 함께 전달한다.
 */
public record NotificationMessage(
    Long userId, // 알람 대상 사용자 식별자
    NotificationType type, // 알람 종류
    NotificationCategory category, // 알람의 카테고리
    String title, // 알람 내용 category에 따라 다름 참조값이 들어간다.
    String content, // 알람 내용 category에 따라 다름 참조값이 들어간다.
    NotificationSourceType sourceType,
    Long sourceId,
    NotificationAction action) {
  public static NotificationMessage create(
      Long userId,
      NotificationType type,
      NotificationCategory category,
      String title,
      String content) {
    return new NotificationMessage(userId, type, category, title, content, null, null, null);
  }

  /**
   * 리뷰 댓글 원본 정보와 {@code OPEN_REVIEW} Action을 포함한 메시지를 생성한다.
   *
   * <p>댓글 식별자는 중복 방지 source와 화면 강조 payload에 함께 사용한다.
   */
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

  /**
   * 관리자 문의 답변의 원본 정보와 {@code OPEN_HELP} Action을 포함한 메시지를 생성한다.
   *
   * <p>문의 식별자는 중복 방지 source와 이동 대상에 함께 사용한다.
   */
  public static NotificationMessage helpAnswer(
      Long userId, Long helpId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationType.USER,
        NotificationCategory.ANSWER,
        title,
        content,
        NotificationSourceType.HELP_ANSWER,
        helpId,
        NotificationAction.openHelp(helpId));
  }
}
