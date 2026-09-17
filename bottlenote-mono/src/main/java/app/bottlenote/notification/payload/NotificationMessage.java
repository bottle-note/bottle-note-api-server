package app.bottlenote.notification.payload;

import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationEventAction;
import java.util.Objects;

public record NotificationMessage(
    Long userId,
    NotificationEventAction eventAction,
    String title,
    String content,
    Long sourceId,
    NotificationAction action) {

  public NotificationMessage {
    Objects.requireNonNull(eventAction, "알림 발생 액션은 필수입니다.");
  }

  public static NotificationMessage create(
      Long userId, NotificationEventAction eventAction, String title, String content) {
    return new NotificationMessage(userId, eventAction, title, content, null, null);
  }

  public String sourceType() {
    return sourceId == null ? null : eventAction.sourceType();
  }

  public static NotificationMessage reviewReply(
      Long userId, Long reviewId, Long replyId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationEventAction.REVIEW_COMMENT,
        title,
        content,
        replyId,
        NotificationAction.openReview(reviewId, replyId));
  }

  public static NotificationMessage reviewReplyResponse(
      Long userId, Long reviewId, Long replyId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationEventAction.REVIEW_REPLY,
        title,
        content,
        replyId,
        NotificationAction.openReview(reviewId, replyId));
  }

  public static NotificationMessage helpAnswer(
      Long userId, Long helpId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationEventAction.HELP_ANSWER,
        title,
        content,
        helpId,
        NotificationAction.openHelp(helpId));
  }

  public static NotificationMessage reviewLike(
      Long userId, Long reviewId, Long likeId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationEventAction.REVIEW_LIKE,
        title,
        content,
        likeId,
        NotificationAction.openReview(reviewId));
  }

  public static NotificationMessage follow(
      Long userId, Long actorId, Long followId, String title, String content) {
    return new NotificationMessage(
        userId,
        NotificationEventAction.FOLLOW,
        title,
        content,
        followId,
        NotificationAction.openUser(actorId));
  }
}
