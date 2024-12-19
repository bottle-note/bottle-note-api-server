package app.bottlenote.picks.event;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.dto.payload.PicksRegistryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static app.bottlenote.history.domain.constant.EventType.IS_PICK;
import static app.bottlenote.history.domain.constant.EventType.UNPICK;
import static app.bottlenote.picks.domain.PicksStatus.PICK;

@Slf4j
@Component
@RequiredArgsConstructor
public class PicksEventPublisher implements HistoryEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	private static final String REDIRECT_URL = "api/v1/picks";
	private static final String MESSAGE = "찜 등록";
	private static final String DESCRIPTION_PICK = "찜 등록되었습니다.";
	private static final String DESCRIPTION_UNPICK = "찜 해제되었습니다.";

	@Override
	public void publishHistoryEvent(Object event) {

		PicksRegistryEvent picksRegistryEvent = (PicksRegistryEvent) event;

		HistoryEvent picksHistoryEvent = HistoryEvent.makeHistoryEvent(
			picksRegistryEvent.userId(),
			EventCategory.PICK,
			picksRegistryEvent.picksStatus() == PICK ? IS_PICK : UNPICK,
			REDIRECT_URL,
			picksRegistryEvent.alcoholId(),
			MESSAGE,
			null,
			picksRegistryEvent.picksStatus() == PICK ? DESCRIPTION_PICK : DESCRIPTION_UNPICK
		);
		eventPublisher.publishEvent(picksHistoryEvent);
	}
}
