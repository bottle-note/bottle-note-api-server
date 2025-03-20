package app.bottlenote.like.event;

import app.bottlenote.history.domain.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.event.payload.LikesRegistryEvent;
import app.bottlenote.review.service.ReviewFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.domain.constant.EventCategory.REVIEW;
import static app.bottlenote.history.domain.constant.EventType.REVIEW_LIKES;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikesEventPublisher implements HistoryEventPublisher {

	private final ReviewFacade reviewFacade;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(Object event) {

		final LikesRegistryEvent likesRegistryEvent = (LikesRegistryEvent) event;
		final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(likesRegistryEvent.reviewId());
		final Long reviewId = likesRegistryEvent.reviewId();

		HistoryEvent likesHistoryEvent = HistoryEvent.builder()
			.userId(likesRegistryEvent.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_LIKES)
			.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
			.alcoholId(alcoholId)
			.build();

		eventPublisher.publishEvent(likesHistoryEvent);
	}
}
