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
	private static final String REDIRECT_URL = "api/v1/reviews";
	private static final String MESSAGE_CREATE = "리뷰 등록";
	private static final String DESCRIPTION_CREATE = "리뷰가 등록되었습니다.";
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(Object event) {
		ReviewRegistryEvent registryEvent = (ReviewRegistryEvent) event;

		//todo: 이렇게 객체가 많은 경우 빌더로 어떤 값들인지 가독성을 높혀주기
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
