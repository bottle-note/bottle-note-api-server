package app.bottlenote.history.event.publisher;


@FunctionalInterface
public interface HistoryEventPublisher {

	void publishHistoryEvent(Object event);
}
