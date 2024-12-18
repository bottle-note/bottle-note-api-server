package app.bottlenote.rating.fixture;

import app.bottlenote.rating.event.publihser.EventPublisher;

public class FakeRatingEventPublisher implements EventPublisher {

	@Override
	public void publishEvent(Object event) {
		// dummy object
	}
}
