package app.bottlenote.rating.event.publihser;

import app.bottlenote.alcohols.service.domain.AlcoholDomainSupport;
import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.rating.dto.payload.RatingRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingEventPublisher {

	private final ApplicationEventPublisher eventPublisher;
	private final AlcoholDomainSupport alcoholDomainSupport;

	public void ratingRegistry(RatingRegistryEvent ratingRegistryEvent) {
		log.info("RatingRegistryEvent: {}", ratingRegistryEvent);

		HistoryEvent ratingCreateHistoryEvent = HistoryEvent.makeHistoryEvent(
			ratingRegistryEvent.userId(),
			EventCategory.RATING,
			EventType.START_RATING,
			"redirectUrl",
			alcoholDomainSupport.findAlcoholImageUrlById(ratingRegistryEvent.alcoholId()),
			ratingRegistryEvent.alcoholId(),
			"별점 등록",
			null,
			"별점이 등록되었습니다."
		);
		eventPublisher.publishEvent(ratingCreateHistoryEvent);
	}

}
