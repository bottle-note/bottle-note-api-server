package app.bottlenote.review.event.publisher;

import static app.bottlenote.history.domain.constant.EventCategory.REVIEW;
import static app.bottlenote.history.domain.constant.EventType.REVIEW_CREATE;

import app.bottlenote.history.domain.constant.RedirectUrlType;
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
public class ReviewEventPublisher implements HistoryEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(Object event) {
		ReviewRegistryEvent registryEvent = (ReviewRegistryEvent) event;

		final Long reviewId = registryEvent.reviewId();

		HistoryEvent reviewCreateHistoryEvent = HistoryEvent.builder()
			.userId(registryEvent.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_CREATE)
			.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
			.alcoholId(registryEvent.alcoholId())
			.content(registryEvent.content())
			.build();
		eventPublisher.publishEvent(reviewCreateHistoryEvent);
	}
}
