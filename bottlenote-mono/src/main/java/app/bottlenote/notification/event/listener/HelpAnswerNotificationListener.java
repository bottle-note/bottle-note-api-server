package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.support.help.event.payload.HelpAnswerNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@link HelpAnswerNotificationEvent}를 구독해 문의 작성자에게 답변 알림을 생성한다.
 *
 * <p>답변 트랜잭션 commit 후 별도 트랜잭션에서 Notification SSOT만 저장한다.
 */
@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class HelpAnswerNotificationListener {

  static final String TITLE = "문의 답변";

  private final NotificationService notificationService;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleHelpAnswerNotification(HelpAnswerNotificationEvent event) {
    if (event == null) {
      return;
    }

    notificationService.sendNotification(
        NotificationMessage.helpAnswer(event.helpUserId(), event.helpId(), TITLE, event.content()));

    log.info("문의 답변 알림 저장 요청 처리 - helpId: {}, helpUserId: {}", event.helpId(), event.helpUserId());
  }
}
