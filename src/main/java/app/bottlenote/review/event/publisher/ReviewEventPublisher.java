package app.bottlenote.review.event.publisher;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
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
	private static final String REDIRECT_URL = "api/v1/reviews";
	private static final String MESSAGE_CREATE = "리뷰 등록";
	private static final String DESCRIPTION_CREATE = "리뷰가 등록되었습니다.";

	@Override
	public void publishHistoryEvent(Object event) {
		ReviewRegistryEvent registryEvent = (ReviewRegistryEvent) event;

		HistoryEvent reviewCreateHistoryEvent = HistoryEvent.makeHistoryEvent(
			registryEvent.userId(),
			EventCategory.REVIEW,
			EventType.REVIEW_CREATE,
			REDIRECT_URL,
			registryEvent.alcoholId(),
			MESSAGE_CREATE,
			null,
			DESCRIPTION_CREATE
		);
		eventPublisher.publishEvent(reviewCreateHistoryEvent);
	}
}
