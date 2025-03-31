package app.bottlenote.rating.fixture;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;

public class FakeRatingEventPublisher implements HistoryEventPublisher<RatingRegistryEvent> {

	@Override
	public void publishHistoryEvent(RatingRegistryEvent event) {
		// dummy object
	}
}
