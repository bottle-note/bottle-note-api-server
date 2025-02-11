package app.bottlenote.like.event;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.dto.payload.LikesRegistryEvent;
import app.bottlenote.review.service.ReviewFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikesEventPublisher implements HistoryEventPublisher {

	private final ReviewFacade reviewFacade;
	private final ApplicationEventPublisher eventPublisher;

	private static final String REDIRECT_URL = "api/v1/likes";

	@Override
	public void publishHistoryEvent(Object event) {

		final LikesRegistryEvent likesRegistryEvent = (LikesRegistryEvent) event;
		final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(likesRegistryEvent.reviewId());

		HistoryEvent likesHistoryEvent = HistoryEvent.builder()
			.userId(likesRegistryEvent.userId())
			.eventCategory(EventCategory.REVIEW)
			.eventType(EventType.REVIEW_LIKES)
			.redirectUrl(REDIRECT_URL)
			.alcoholId(alcoholId)
			.build();

		eventPublisher.publishEvent(likesHistoryEvent);
	}
}
