package app.bottlenote.common.file.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.service.ResourceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class ResourceEventListener {

  private final ResourceCommandService resourceCommandService;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleImageResourceActivated(ImageResourceActivatedEvent event) {
    log.info(
        "이미지 리소스 활성화 이벤트 수신 - referenceId: {}, referenceType: {}, resourceKeys: {}",
        event.referenceId(),
        event.referenceType(),
        event.resourceKeys().size());

    for (String resourceKey : event.resourceKeys()) {
      resourceCommandService.activateImageResource(
          resourceKey, event.referenceId(), event.referenceType());
    }
  }
}
