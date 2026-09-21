package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class ReviewReplyNotificationListener {

  static final String TITLE = "새 댓글";
  static final String REPLY_TITLE = "새 답글";

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewReplyNotification(ReviewReplyActivityEvent event) {
    if (event == null) {
      return;
    }

    try {
      if (event.parentReplyUserId() != null) {
        sendParentReply(event);
        return;
      }
      sendReviewComment(event);
    } catch (RuntimeException exception) {
      log.error("댓글 알림 처리 실패 - replyId: {}", event.replyId(), exception);
      throw exception;
    }
  }

  private void sendParentReply(ReviewReplyActivityEvent event) {
    if (Objects.equals(event.parentReplyUserId(), event.replyUserId())) {
      return;
    }
    notificationService.sendNotification(
        NotificationMessage.reviewReplyResponse(
            event.parentReplyUserId(),
            event.reviewId(),
            event.replyId(),
            REPLY_TITLE,
            event.content()));
  }

  private void sendReviewComment(ReviewReplyActivityEvent event) {
    if (Objects.equals(event.reviewAuthorId(), event.replyUserId())) {
      return;
    }
    notificationService.sendNotification(
        NotificationMessage.reviewReply(
            event.reviewAuthorId(), event.reviewId(), event.replyId(), TITLE, event.content()));
  }
}
