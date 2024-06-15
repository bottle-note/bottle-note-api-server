package app.bottlenote.review.service;

import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.review.domain.event.ReviewReplyRegistryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ReviewReplyEventHandler {

	private final UserHistoryRepository userHistoryRepository;

	public ReviewReplyEventHandler(UserHistoryRepository userHistoryRepository) {
		this.userHistoryRepository = userHistoryRepository;
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener
	public void registryReviewReply(
		ReviewReplyRegistryEvent reply
	) {
		log.info("이벤트 발행 :: ReviewReplyEventHandler.registryReviewReply :: {}", reply);
	}
}
