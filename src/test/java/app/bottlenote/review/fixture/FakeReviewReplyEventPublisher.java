package app.bottlenote.review.fixture;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;

public class FakeReviewReplyEventPublisher implements HistoryEventPublisher {

	@Override
	public void publishHistoryEvent(Object event) {
		//dummy object
	}
}
