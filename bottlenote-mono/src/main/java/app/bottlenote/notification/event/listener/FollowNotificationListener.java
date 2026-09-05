package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.user.event.payload.FollowActivityEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class FollowNotificationListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(FollowActivityEvent event) {
    if (event == null || Objects.equals(event.targetUserId(), event.actorId())) {
      return;
    }
    notificationService.sendNotification(
        NotificationMessage.follow(
            event.targetUserId(), event.actorId(), event.followId(), "새 팔로워", "새로운 팔로워가 생겼습니다."));
  }
}
