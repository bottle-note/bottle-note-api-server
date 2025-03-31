package app.bottlenote.picks.event;

import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.constant.EventType.IS_PICK;
import static app.bottlenote.history.constant.EventType.UNPICK;
import static app.bottlenote.picks.constant.PicksStatus.PICK;

@Slf4j
@Component
@RequiredArgsConstructor
public class PicksEventPublisher implements HistoryEventPublisher<PicksRegistryEvent> {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void publishHistoryEvent(PicksRegistryEvent event) {

		final Long alcoholId = event.alcoholId();

		HistoryEvent picksHistoryEvent = HistoryEvent.builder()
				.userId(event.userId())
			.eventCategory(EventCategory.PICK)
				.eventType(event.picksStatus() == PICK ? IS_PICK : UNPICK)
			.redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
				.alcoholId(event.alcoholId())
			.build();
		eventPublisher.publishEvent(picksHistoryEvent);
	}
}
