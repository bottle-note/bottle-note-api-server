package app.bottlenote.history.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.SYNCHRONOUS;

import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

@RequiredArgsConstructor
@DomainEventListener(type = SYNCHRONOUS)
public class HistoryRegistryListener {

  private final HistoryEventPublisher historyEventPublisher;

  // 원본 트랜잭션에서 변환하고 기존 AFTER_COMMIT 저장 경로로 전달한다.
  @EventListener
  public void handleReviewRegistry(ReviewRegistryEvent event) {
    historyEventPublisher.publishReviewHistoryEvent(event);
  }

  @EventListener
  public void handlePicksRegistry(PicksRegistryEvent event) {
    historyEventPublisher.publishPicksHistoryEvent(event);
  }

  @EventListener
  public void handleRatingRegistry(RatingRegistryEvent event) {
    historyEventPublisher.publishRatingHistoryEvent(event);
  }
}
