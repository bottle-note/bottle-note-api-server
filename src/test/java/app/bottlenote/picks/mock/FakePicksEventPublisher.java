package app.bottlenote.picks.mock;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;

public class FakePicksEventPublisher implements HistoryEventPublisher {

	@Override
	public void publishHistoryEvent(Object event) {
		//Dummy Object
	}
}
