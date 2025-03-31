package app.bottlenote.picks.fake;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;

public class FakePicksEventPublisher implements HistoryEventPublisher<PicksRegistryEvent> {

	@Override
	public void publishHistoryEvent(PicksRegistryEvent event) {
		//todo :  실제 이벤트 발행을 인지할 수 있도록 구현 필요
		//Dummy Object
	}
}
