package app.bottlenote.review.event.publisher;

import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.review.dto.payload.ReviewRegistryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewEventPublisher implements ApplicationEventPublisher {

	@Override
	public void publishEvent(ApplicationEvent event) {
		ApplicationEventPublisher.super.publishEvent(event);
	}

	@Override
	public void publishEvent(final Object event) {
	}

	public void reviewRegistry(ReviewRegistryEvent registryEvent) {
		log.info("ReviewRegistryEvent: {}", registryEvent);

		//ReviewRegistryEvent 를  HistoryEvent 로 변경 해서 이벤트 발행

		publishEvent(registryEvent);
	}

	public void reviewUpdate(Object o) {
		log.info("ReviewUpdateEvent: {}", o);
		publishEvent(o);
	}


}
