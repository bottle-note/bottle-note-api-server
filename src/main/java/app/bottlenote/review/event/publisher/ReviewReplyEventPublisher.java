package app.bottlenote.review.event.publisher;

import static app.bottlenote.history.domain.constant.EventCategory.REVIEW;
import static app.bottlenote.history.domain.constant.EventType.REVIEW_REPLY_CREATE;

import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.review.dto.payload.ReviewRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReplyEventPublisher implements HistoryEventPublisher {

	private static final String REDIRECT_URL = "api/v1/review/reply";
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(Object event) {
		ReviewRegistryEvent registryEvent = (ReviewRegistryEvent) event;

		HistoryEvent reviewReplyHistoryEvent = HistoryEvent.builder()
			.userId(registryEvent.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_REPLY_CREATE)
			.redirectUrl(REDIRECT_URL)
			.alcoholId(registryEvent.alcoholId())
			.content(registryEvent.content())
			.build();
		eventPublisher.publishEvent(reviewReplyHistoryEvent);
	}
}
