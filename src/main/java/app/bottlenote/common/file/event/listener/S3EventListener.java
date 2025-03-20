package app.bottlenote.common.file.event.listener;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.common.file.dto.event.S3RequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class S3EventListener {

	/**
	 * S3 요청 이벤트 기록 처리
	 *
	 * @param event the event
	 */
	//@TransactionalEventListener
	@EventListener
	public void handleS3RequestEvent(final S3RequestEvent event) {
		log.info("S3 Request Event: {}", event);
		//현재 쓰레드 출력
		log.info("Current Thread: {}", Thread.currentThread().getName());
	}
}
