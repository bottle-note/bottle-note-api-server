package app.bottlenote.review.event.publisher;

import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.constant.EventCategory.REVIEW;
import static app.bottlenote.history.constant.EventType.REVIEW_CREATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventPublisher implements HistoryEventPublisher<ReviewRegistryEvent> {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(ReviewRegistryEvent event) {

		final Long reviewId = event.reviewId();

		HistoryEvent reviewCreateHistoryEvent = HistoryEvent.builder()
				.userId(event.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_CREATE)
			.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
				.alcoholId(event.alcoholId())
				.content(event.content())
			.build();
		eventPublisher.publishEvent(reviewCreateHistoryEvent);
	}
}
