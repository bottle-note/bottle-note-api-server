package app.bottlenote.review.service.event;

import app.bottlenote.review.domain.event.ReviewReplyRegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReviewReplyEventHandler {
	private static final Logger log = LogManager.getLogger(ReviewReplyEventHandler.class);

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(
		value = ReviewReplyRegistryEvent.class,
		phase = TransactionPhase.AFTER_COMMIT
	)
	public void registryReviewReply(
		ReviewReplyRegistryEvent reply
	) {
		System.out.println("이벤트 발행 :: ReviewReplyEventHandler.registryReviewReply :: " + reply);
		log.info("이벤트 발행 :: ReviewReplyEventHandler.registryReviewReply :: {}", reply);
	}
}
