package app.bottlenote.review.event.publisher;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.review.dto.payload.ReviewRegistryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewEventPublisher {
	private final ApplicationEventPublisher eventPublisher;

	public ReviewEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	public void reviewRegistry(ReviewRegistryEvent registryEvent) {
		log.info("ReviewRegistryEvent: {}", registryEvent);

		HistoryEvent reviewCreateHistoryEvent = HistoryEvent.makeHistoryEvent(
			registryEvent.userId(),
			EventCategory.REVIEW,
			EventType.REVIEW_CREATE,
			"redirectUrl",
			registryEvent.alcoholId(),
			"리뷰 등록",
			null,
			"리뷰가 등록되었습니다."
		);
		eventPublisher.publishEvent(reviewCreateHistoryEvent);
	}

	public void reviewUpdate(Object o) {
		log.info("ReviewUpdateEvent: {}", o);
		eventPublisher.publishEvent(o);
	}
}
