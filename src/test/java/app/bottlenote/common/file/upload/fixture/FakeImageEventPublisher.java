package app.bottlenote.common.file.upload.fixture;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public class FakeImageEventPublisher implements ApplicationEventPublisher {

	@Override
	public void publishEvent(ApplicationEvent event) {
		ApplicationEventPublisher.super.publishEvent(event);
	}

	@Override
	public void publishEvent(Object event) {
		System.out.println("Fake 이벤트 발생");
	}
}
