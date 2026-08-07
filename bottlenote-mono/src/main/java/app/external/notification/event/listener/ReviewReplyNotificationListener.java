package app.external.notification.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.review.event.payload.ReviewReplyRegistryEvent;
import app.external.notification.application.NotificationService;
import app.external.notification.data.payload.NotificationMessage;
import app.external.notification.domain.constant.NotificationCategory;
import app.external.notification.domain.constant.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** 리뷰 댓글 등록 시 리뷰 작성자에게 Notification(SSOT)을 생성한다. Delivery(SSE/Push)는 담당하지 않는다. */
@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class ReviewReplyNotificationListener {

  static final String TITLE = "새 댓글";

  private final NotificationService notificationService;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleReviewReplyRegistered(ReviewReplyRegistryEvent event) {
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
        NotificationMessage.create(
            event.reviewAuthorId(),
            NotificationType.USER,
            NotificationCategory.REVIEW,
            TITLE,
            event.content());

    notificationService.sendNotification(message);

    log.info(
        "리뷰 댓글 알림 생성 - reviewId: {}, reviewAuthorId: {}, replyUserId: {}, replyId: {}",
        event.reviewId(),
        event.reviewAuthorId(),
        event.replyUserId(),
        event.replyId());
  }
}
