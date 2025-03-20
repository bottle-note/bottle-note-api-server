package app.bottlenote.review.event.publisher;

import app.bottlenote.history.domain.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.domain.constant.EventCategory.REVIEW;
import static app.bottlenote.history.domain.constant.EventType.REVIEW_REPLY_CREATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReplyEventPublisher implements HistoryEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(Object event) {
		ReviewRegistryEvent registryEvent = (ReviewRegistryEvent) event;

		final Long reviewId = registryEvent.reviewId();

		HistoryEvent reviewReplyHistoryEvent = HistoryEvent.builder()
			.userId(registryEvent.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_REPLY_CREATE)
			.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
			.alcoholId(registryEvent.alcoholId())
			.content(registryEvent.content())
			.build();
		eventPublisher.publishEvent(reviewReplyHistoryEvent);
	}
}
