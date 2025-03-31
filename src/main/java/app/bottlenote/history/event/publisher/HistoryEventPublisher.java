package app.bottlenote.history.event.publisher;


@FunctionalInterface
public interface HistoryEventPublisher<T> {

	void publishHistoryEvent(T event);
}

