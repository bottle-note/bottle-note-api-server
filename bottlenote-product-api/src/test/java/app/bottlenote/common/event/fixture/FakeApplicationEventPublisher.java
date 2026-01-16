package app.bottlenote.common.event.fixture;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public class FakeApplicationEventPublisher implements ApplicationEventPublisher {

  private final List<Object> publishedEvents = new ArrayList<>();

  @Override
  public void publishEvent(ApplicationEvent event) {
    publishedEvents.add(event);
  }

  @Override
  public void publishEvent(Object event) {
    publishedEvents.add(event);
  }

  public List<Object> getPublishedEvents() {
    return List.copyOf(publishedEvents);
  }

  public <T> List<T> getPublishedEventsOfType(Class<T> eventType) {
    return publishedEvents.stream().filter(eventType::isInstance).map(eventType::cast).toList();
  }

  public void clear() {
    publishedEvents.clear();
  }

  public int getPublishedEventCount() {
    return publishedEvents.size();
  }

  public boolean hasPublishedEventOfType(Class<?> eventType) {
    return publishedEvents.stream().anyMatch(eventType::isInstance);
  }
}
