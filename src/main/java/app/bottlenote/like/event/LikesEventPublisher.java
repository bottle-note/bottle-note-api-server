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
	private static final String MESSAGE = "리뷰 좋아요";
	private static final String DESCRIPTION_LIKE = "리뷰 좋아요가 등록되었습니다.";

	@Override
	public void publishHistoryEvent(Object event) {

		final LikesRegistryEvent likesRegistryEvent = (LikesRegistryEvent) event;
		final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(likesRegistryEvent.reviewId());

		HistoryEvent likesHistoryEvent = HistoryEvent.makeHistoryEvent(
			likesRegistryEvent.userId(),
			EventCategory.REVIEW,
			EventType.REVIEW_LIKES,
			REDIRECT_URL,
			alcoholId,
			MESSAGE,
			null,
			DESCRIPTION_LIKE
		);

		eventPublisher.publishEvent(likesHistoryEvent);
	}
}
