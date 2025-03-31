package app.bottlenote.review.fixture;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;

public class FakeReviewReplyEventPublisher implements HistoryEventPublisher<ReviewRegistryEvent> {

	@Override
	public void publishHistoryEvent(ReviewRegistryEvent event) {
		//dummy object
	}
}
