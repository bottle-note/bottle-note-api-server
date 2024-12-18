package app.bottlenote.rating.fixture;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;

public class FakeRatingEventPublisher implements HistoryEventPublisher {
	
	@Override
	public void publishHistoryEvent(Object event) {
		// dummy object
	}
}
