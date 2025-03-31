package app.bottlenote.like.fake;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.event.payload.LikesRegistryEvent;

public class FakeLikesEventPublisher implements HistoryEventPublisher<LikesRegistryEvent> {

	@Override
	public void publishHistoryEvent(LikesRegistryEvent event) {
		//dummy object
	}
}
