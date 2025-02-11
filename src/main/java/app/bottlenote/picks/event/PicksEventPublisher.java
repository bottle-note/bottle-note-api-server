package app.bottlenote.picks.event;

import static app.bottlenote.history.domain.constant.EventType.IS_PICK;
import static app.bottlenote.history.domain.constant.EventType.UNPICK;
import static app.bottlenote.picks.domain.PicksStatus.PICK;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.dto.payload.PicksRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PicksEventPublisher implements HistoryEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	private static final String REDIRECT_URL = "api/v1/picks";

	@Override
	public void publishHistoryEvent(Object event) {

		PicksRegistryEvent picksRegistryEvent = (PicksRegistryEvent) event;

		HistoryEvent picksHistoryEvent = HistoryEvent.builder()
			.userId(picksRegistryEvent.userId())
			.eventCategory(EventCategory.PICK)
			.eventType(picksRegistryEvent.picksStatus() == PICK ? IS_PICK : UNPICK)
			.redirectUrl(REDIRECT_URL)
			.alcoholId(picksRegistryEvent.alcoholId())
			.build();
		eventPublisher.publishEvent(picksHistoryEvent);
	}
}
