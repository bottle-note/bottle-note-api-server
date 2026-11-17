package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class ReviewLikeNotificationListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ReviewLikeActivityEvent event) {
    if (event == null
        || !event.activated()
        || Objects.equals(event.reviewAuthorId(), event.actorId())) {
      return;
    }
    notificationService.sendNotification(
        NotificationMessage.reviewLike(
            event.reviewAuthorId(), event.reviewId(), event.likeId(), "새 좋아요", event.content()));
  }
}
