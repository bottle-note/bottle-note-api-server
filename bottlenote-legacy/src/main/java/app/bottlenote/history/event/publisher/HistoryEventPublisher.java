package app.bottlenote.history.event.publisher;

import static app.bottlenote.shared.history.constant.EventCategory.RATING;
import static app.bottlenote.shared.history.constant.EventCategory.REVIEW;
import static app.bottlenote.shared.history.constant.EventType.IS_PICK;
import static app.bottlenote.shared.history.constant.EventType.REVIEW_CREATE;
import static app.bottlenote.shared.history.constant.EventType.REVIEW_LIKES;
import static app.bottlenote.shared.history.constant.EventType.REVIEW_REPLY_CREATE;
import static app.bottlenote.shared.history.constant.EventType.UNPICK;
import static app.bottlenote.shared.picks.constant.PicksStatus.PICK;

import app.bottlenote.core.review.application.ReviewFacade;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.like.event.payload.LikesRegistryEvent;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;
import app.bottlenote.shared.history.constant.EventCategory;
import app.bottlenote.shared.history.constant.EventType;
import app.bottlenote.shared.history.constant.RedirectUrlType;
import app.bottlenote.shared.review.payload.ReviewRegistryEvent;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryEventPublisher {

  private final ApplicationEventPublisher eventPublisher;
  private final ReviewFacade reviewFacade;

  public void publishReviewHistoryEvent(ReviewRegistryEvent event) {
    final Long reviewId = event.reviewId();

    HistoryEvent reviewCreateHistoryEvent =
        HistoryEvent.builder()
            .userId(event.userId())
            .eventCategory(REVIEW)
            .eventType(REVIEW_CREATE)
            .redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
            .alcoholId(event.alcoholId())
            .content(event.content())
            .build();
    eventPublisher.publishEvent(reviewCreateHistoryEvent);
  }

  public void publishReplyHistoryEvent(ReviewRegistryEvent event) {

    final Long reviewId = event.reviewId();

    HistoryEvent reviewReplyHistoryEvent =
        HistoryEvent.builder()
            .userId(event.userId())
            .eventCategory(REVIEW)
            .eventType(REVIEW_REPLY_CREATE)
            .redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
            .alcoholId(event.alcoholId())
            .content(event.content())
            .build();
    eventPublisher.publishEvent(reviewReplyHistoryEvent);
  }

  public void publishLikesHistoryEvent(LikesRegistryEvent event) {
    final Long alcoholId = reviewFacade.getAlcoholIdByReviewId(event.reviewId());
    final Long reviewId = event.reviewId();

    HistoryEvent likesHistoryEvent =
        HistoryEvent.builder()
            .userId(event.userId())
            .eventCategory(REVIEW)
            .eventType(REVIEW_LIKES)
            .redirectUrl(RedirectUrlType.REVIEW.getUrl() + "/" + reviewId)
            .alcoholId(alcoholId)
            .content(event.content())
            .build();

    eventPublisher.publishEvent(likesHistoryEvent);
  }

  public void publishPicksHistoryEvent(PicksRegistryEvent event) {
    final Long alcoholId = event.alcoholId();

    HistoryEvent picksHistoryEvent =
        HistoryEvent.builder()
            .userId(event.userId())
            .eventCategory(EventCategory.PICK)
            .eventType(event.picksStatus() == PICK ? IS_PICK : UNPICK)
            .redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
            .alcoholId(event.alcoholId())
            .build();
    eventPublisher.publishEvent(picksHistoryEvent);
  }

  public void publishRatingHistoryEvent(RatingRegistryEvent event) {
    final Long alcoholId = event.alcoholId();
    final boolean isUpdate = !Objects.isNull(event.prevRating());
    double prevRatingPoint = 0.0;

    if (isUpdate) {
      prevRatingPoint = event.prevRating().getRating();
    }
    Double currentRatingPoint = event.currentRating().getRating();

    log.info("isUpdate : {}", isUpdate);

    HistoryEvent ratingCreateHistoryEvent =
        HistoryEvent.builder()
            .userId(event.userId())
            .eventCategory(RATING)
            .eventType(makeEventType(isUpdate, currentRatingPoint))
            .redirectUrl(RedirectUrlType.ALCOHOL.getUrl() + "/" + alcoholId)
            .alcoholId(event.alcoholId())
            .dynamicMessage(
                isUpdate
                    ? makeDynamicMessage(currentRatingPoint, prevRatingPoint)
                    : Map.of("currentValue", currentRatingPoint.toString()))
            .build();
    eventPublisher.publishEvent(ratingCreateHistoryEvent);
  }

  public Map<String, String> makeDynamicMessage(Double currentRating, Double prevRating) {
    return Map.of(
        "currentValue", currentRating.toString(),
        "prevValue", prevRating.toString(),
        "ratingDiff", Double.toString(currentRating - prevRating));
  }

  private EventType makeEventType(boolean isUpdate, Double currentRatingPoint) {
    if (isUpdate) {
      if (currentRatingPoint == 0.0) {
        return EventType.RATING_DELETE;
      } else {
        return EventType.RATING_MODIFY;
      }
    } else {
      return EventType.START_RATING;
    }
  }
}
