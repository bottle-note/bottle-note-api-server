package app.bottlenote.picks.mock;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;

public class FakePicksEventPublisher implements HistoryEventPublisher {

	@Override
	public void publishHistoryEvent(Object event) {
		//todo :  실제 이벤트 발행을 인지할 수 있도록 구현 필요
		//Dummy Object
	}
}
