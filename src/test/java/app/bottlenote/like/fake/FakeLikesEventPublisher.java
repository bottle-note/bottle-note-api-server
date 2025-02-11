package app.bottlenote.like.fake;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;

public class FakeLikesEventPublisher implements HistoryEventPublisher {

	@Override
	public void publishHistoryEvent(Object event) {
		//dummy object
	}
}
