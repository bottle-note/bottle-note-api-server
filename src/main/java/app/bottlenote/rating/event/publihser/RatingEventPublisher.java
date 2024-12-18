package app.bottlenote.rating.event.publihser;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.rating.dto.payload.RatingRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingEventPublisher implements EventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	private static final String REDIRECT_URL = "api/v1/rating";
	private static final String MESSAGE = "별점 등록";
	private static final String DESCRIPTION_REGISTER = "별점이 등록되었습니다.";
	private static final String DESCRIPTION_UPDATE = "별점이 수정되었습니다.";

	private RatingRegistryEvent ratingRegistryEvent;
	private Double prevRatingPoint;

	@Override
	public void publishEvent(Object event) {
		log.info("RatingRegistryEvent: {}", ratingRegistryEvent);
		ratingRegistryEvent = (RatingRegistryEvent) event;
		
		// 기존 등록된 별점이 있어서 이벤트 페이로드로 이전 별점 정보를 넘긴 상황 -> 수정
		boolean isUpdate = !Objects.isNull((ratingRegistryEvent).prevRating());

		if (isUpdate) {
			prevRatingPoint = ratingRegistryEvent.prevRating().getRating();
		}
		Double currentRatingPoint = ratingRegistryEvent.currentRating().getRating();

		log.info("isUpdate : {}", isUpdate);

		HistoryEvent ratingCreateHistoryEvent = HistoryEvent.makeHistoryEvent(
			ratingRegistryEvent.userId(),
			EventCategory.RATING,
			makeEventType(isUpdate, currentRatingPoint),
			REDIRECT_URL,
			ratingRegistryEvent.alcoholId(),
			MESSAGE,
			isUpdate ? makeDynamicMessage(currentRatingPoint, prevRatingPoint) : Map.of("currentValue", currentRatingPoint),
			isUpdate ? DESCRIPTION_UPDATE : DESCRIPTION_REGISTER
		);
		eventPublisher.publishEvent(ratingCreateHistoryEvent);
	}

	public Map<String, Object> makeDynamicMessage(Double currentRating, Double prevRating) {
		return Map.of(
			"currentValue", currentRating,
			"prevValue", prevRating,
			"ratingDiff", currentRating - prevRating
		);
	}

	private EventType makeEventType(boolean isUpdate, Double currentRatingPoint) {
		if (isUpdate) {
			if (currentRatingPoint == 0.0) {
				return EventType.RATING_DELETE;
			} else {
				return EventType.RATING_MODIFY;
			}
		} else {
			return EventType.START_RATING;
		}
	}
}
