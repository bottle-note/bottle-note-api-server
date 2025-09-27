package app.bottlenote.history.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.alcohols.service.AlcoholFacade;
import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.event.payload.HistoryEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class HistoryListener {

  private final AlcoholFacade alcoholFacade;
  private final UserHistoryRepository userHistoryRepository;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handleUserHistoryRegistry(HistoryEvent event) {
    String alcoholImageUrl = alcoholFacade.findAlcoholImageUrlById(event.alcoholId()).orElse(null);

    UserHistory save =
        userHistoryRepository.save(
            UserHistory.builder()
                .userId(event.userId())
                .alcoholId(event.alcoholId())
                .eventCategory(event.eventCategory())
                .eventType(event.eventType())
                .redirectUrl(event.redirectUrl())
                .imageUrl(alcoholImageUrl)
                .content(event.content())
                .dynamicMessage(event.dynamicMessage())
                .eventYear(String.valueOf(LocalDateTime.now().getYear()))
                .eventMonth(String.valueOf(LocalDateTime.now().getMonth()))
                .build());

    log.debug("History saved: {}", save);
  }
}
