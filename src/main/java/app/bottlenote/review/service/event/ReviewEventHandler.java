package app.bottlenote.review.service.event;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.service.HistoryDomainSupport;
import app.bottlenote.review.domain.event.ReviewRegistryEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

import static app.bottlenote.history.domain.constant.EventType.REVIEW_CREATE;

@Component
public class ReviewEventHandler {
	
	private final HistoryDomainSupport historyDomainSupport;

	public ReviewEventHandler(HistoryDomainSupport historyDomainSupport) {
		this.historyDomainSupport = historyDomainSupport;
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(
		value = ReviewRegistryEvent.class,
		phase = TransactionPhase.AFTER_COMMIT
	)
	public void registryReview(
		ReviewRegistryEvent registryEvent
	){
		UserHistory history = UserHistory.builder()
			.userId(registryEvent.userId())
			.alcoholId(registryEvent.alcoholId())
			.eventCategory(REVIEW_CREATE.getEventCategory())
			.eventType(REVIEW_CREATE)
			.redirectUrl("api/v1/reviews/"+registryEvent.alcoholId())
			.message("")
			.dynamicMessage(Map.of())
			.eventYear(String.valueOf(LocalDateTime.now().getYear()))
			.eventMonth(String.valueOf(LocalDateTime.now().getMonth()))
			.description("")
			.build();

		historyDomainSupport.saveHistory(history);
	}
}
