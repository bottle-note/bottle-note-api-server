package app.bottlenote.review.service.event;

import app.bottlenote.review.domain.event.ReviewReplyRegistryEvent;
import app.bottlenote.user.domain.constant.NotificationCategory;
import app.bottlenote.user.domain.constant.NotificationType;
import app.bottlenote.user.dto.request.NotificationMessage;
import app.bottlenote.user.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReviewReplyEventHandler {
	private static final Logger log = LogManager.getLogger(ReviewReplyEventHandler.class);
	private final NotificationService notificationService;

	public ReviewReplyEventHandler(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(
		value = ReviewReplyRegistryEvent.class,
		phase = TransactionPhase.AFTER_COMMIT
	)
	public void registryReviewReply(
		ReviewReplyRegistryEvent reply
	) {
		log.info("[Event]리뷰 댓글 이벤트 발행 :: ReviewReplyEventHandler.registryReviewReply :: {}  thread name : {}", reply, Thread.currentThread().getName());
		NotificationMessage message = NotificationMessage.create(
			reply.userId(),
			NotificationType.USER,
			NotificationCategory.REVIEW,
			String.format("리뷰에 댓글이 등록 되었습니다. %s", reply.content()),
			String.format("@reviewId=%d @content=%s", reply.reviewId(), reply.content())
		);
		notificationService.sendNotification(message);
	}
}
