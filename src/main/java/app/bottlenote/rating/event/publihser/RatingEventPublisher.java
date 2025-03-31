package app.bottlenote.rating.event.publihser;

import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static app.bottlenote.history.constant.EventCategory.RATING;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingEventPublisher implements HistoryEventPublisher<RatingRegistryEvent> {

	private final ApplicationEventPublisher eventPublisher;

	private Double prevRatingPoint;

	@Override
	public void publishHistoryEvent(RatingRegistryEvent event) {
		log.info("RatingRegistryEvent: {}", event);
		final Long alcoholId = event.alcoholId();

		// 기존 등록된 별점이 있어서 이벤트 페이로드로 이전 별점 정보를 넘긴 상황 -> 수정
		final boolean isUpdate = !Objects.isNull(event.prevRating());

		if (isUpdate) {
			prevRatingPoint = event.prevRating().getRating();
		}
		Double currentRatingPoint = event.currentRating().getRating();

		log.info("isUpdate : {}", isUpdate);

		HistoryEvent ratingCreateHistoryEvent = HistoryEvent.builder()
				.userId(event.userId())
			.eventCategory(RATING)
			.eventType(makeEventType(isUpdate, currentRatingPoint))
			.redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
				.alcoholId(event.alcoholId())
			.dynamicMessage(isUpdate ? makeDynamicMessage(currentRatingPoint, prevRatingPoint) : Map.of("currentValue", currentRatingPoint.toString()))
			.build();
		eventPublisher.publishEvent(ratingCreateHistoryEvent);
	}

	public Map<String, String> makeDynamicMessage(Double currentRating, Double prevRating) {
		return Map.of(
			"currentValue", currentRating.toString(),
			"prevValue", prevRating.toString(),
			"ratingDiff", Double.toString(currentRating - prevRating)
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
