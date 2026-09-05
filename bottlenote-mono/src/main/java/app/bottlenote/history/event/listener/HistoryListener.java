package app.bottlenote.history.event.listener;

import static app.bottlenote.common.annotation.DomainEventListener.ProcessingType.ASYNCHRONOUS;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.common.annotation.DomainEventListener;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@DomainEventListener(type = ASYNCHRONOUS)
public class HistoryListener {

  private final AlcoholFacade alcoholFacade;
  private final UserHistoryRepository userHistoryRepository;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserHistoryRegistry(HistoryEvent event) {
    saveHistory(event);
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewReplyActivity(ReviewReplyActivityEvent event) {
    saveHistory(
        HistoryEvent.builder()
            .userId(event.replyUserId())
            .eventCategory(EventCategory.REVIEW)
            .eventType(EventType.REVIEW_REPLY_CREATE)
            .redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + event.reviewId())
            .alcoholId(event.alcoholId())
            .content(event.content())
            .build());
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewLikeActivity(ReviewLikeActivityEvent event) {
    // 기존 History는 취소와 동일 상태 반복 요청도 기록한다.
    saveHistory(
        HistoryEvent.builder()
            .userId(event.actorId())
            .eventCategory(EventCategory.REVIEW)
            .eventType(EventType.REVIEW_LIKES)
            .redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + event.reviewId())
            .alcoholId(event.alcoholId())
            .content(event.content())
            .build());
  }

  private void saveHistory(HistoryEvent event) {
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
