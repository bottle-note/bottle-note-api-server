package app.bottlenote.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.review.event.payload.ReviewReplyNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@link ReviewReplyNotificationEvent}를 구독해 리뷰 작성자에게 Notification(SSOT)을 생성한다.
 *
 * <p>Delivery(SSE/Push)는 담당하지 않는다. History 발행 경로와 분리되어 있으며, 이후 활동 이벤트 파이프라인
 * 공동화(bottle-note/workspace#373) 때 단일 이벤트 리스너 분기로 합쳐질 수 있다.
 */
@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class ReviewReplyNotificationListener {

  static final String TITLE = "새 댓글";

  private final NotificationService notificationService;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewReplyNotification(ReviewReplyNotificationEvent event) {
    if (event == null) {
      return;
    }

    if (event.isSelfReply()) {
      log.debug(
          "본인 리뷰 댓글 알림 생략 - reviewId: {}, userId: {}, replyId: {}",
          event.reviewId(),
          event.replyUserId(),
          event.replyId());
      return;
    }

    NotificationMessage message =
        NotificationMessage.reviewReply(
            event.reviewAuthorId(),
            event.reviewId(),
            event.replyId(),
            TITLE,
            event.content());

    notificationService.sendNotification(message);

    log.info(
        "리뷰 댓글 알림 저장 요청 처리 - reviewId: {}, reviewAuthorId: {}, replyUserId: {}, replyId: {}",
        event.reviewId(),
        event.reviewAuthorId(),
        event.replyUserId(),
        event.replyId());
  }
}
