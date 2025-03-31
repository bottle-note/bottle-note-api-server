package app.bottlenote.like.event;

import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.event.payload.LikesRegistryEvent;
import app.bottlenote.review.facade.ReviewFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.constant.EventCategory.REVIEW;
import static app.bottlenote.history.constant.EventType.REVIEW_LIKES;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikesEventPublisher implements HistoryEventPublisher<LikesRegistryEvent> {

	private final ReviewFacade reviewFacade;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(LikesRegistryEvent event) {

		final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(event.reviewId());
		final Long reviewId = event.reviewId();

		HistoryEvent likesHistoryEvent = HistoryEvent.builder()
				.userId(event.userId())
			.eventCategory(REVIEW)
			.eventType(REVIEW_LIKES)
			.redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
			.alcoholId(alcoholId)
			.build();

		eventPublisher.publishEvent(likesHistoryEvent);
	}
}
