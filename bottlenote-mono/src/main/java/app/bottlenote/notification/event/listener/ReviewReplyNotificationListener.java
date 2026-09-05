package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import java.util.ArrayList;
import java.util.List;
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

    List<RuntimeException> failures = new ArrayList<>();
    boolean sameRecipient = Objects.equals(event.reviewAuthorId(), event.parentReplyUserId());
    if (!Objects.equals(event.reviewAuthorId(), event.replyUserId()) && !sameRecipient) {
      sendReviewComment(event, failures);
    }
    if (event.parentReplyUserId() != null
        && !Objects.equals(event.parentReplyUserId(), event.replyUserId())) {
      try {
        notificationService.sendNotification(
            NotificationMessage.reviewReplyResponse(
                event.parentReplyUserId(), event.reviewId(), event.replyId(), REPLY_TITLE,
                event.content()));
      } catch (RuntimeException exception) {
        failures.add(exception);
      }
    }

    if (!failures.isEmpty()) {
      log.error("댓글 알림 일부 처리 실패 - replyId: {}, failureCount: {}", event.replyId(), failures.size());
      IllegalStateException failure = new IllegalStateException("댓글 알림 수신자 처리 실패");
      failures.forEach(failure::addSuppressed);
      throw failure;
    }
  }

  private void sendReviewComment(ReviewReplyActivityEvent event, List<RuntimeException> failures) {
    try {
      notificationService.sendNotification(
          NotificationMessage.reviewReply(
              event.reviewAuthorId(), event.reviewId(), event.replyId(), TITLE, event.content()));
    } catch (RuntimeException exception) {
      // 수신자별 새 트랜잭션 실패를 모아 나머지 전달을 마친 뒤 보고한다.
      failures.add(exception);
    }
  }
}
