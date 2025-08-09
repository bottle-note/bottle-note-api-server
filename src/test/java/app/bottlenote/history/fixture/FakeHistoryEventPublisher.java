package app.bottlenote.history.fixture;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.event.payload.LikesRegistryEvent;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;
import java.util.ArrayList;
import java.util.List;

public class FakeHistoryEventPublisher extends HistoryEventPublisher {

  public final List<ReviewRegistryEvent> reviewEvents = new ArrayList<>();
  public final List<ReviewRegistryEvent> replyEvents = new ArrayList<>();
  public final List<LikesRegistryEvent> likesEvents = new ArrayList<>();
  public final List<PicksRegistryEvent> picksEvents = new ArrayList<>();
  public final List<RatingRegistryEvent> ratingEvents = new ArrayList<>();

  public FakeHistoryEventPublisher() {
    super(null, null);
  }

  @Override
  public void publishReviewHistoryEvent(ReviewRegistryEvent event) {
    reviewEvents.add(event);
  }

  @Override
  public void publishReplyHistoryEvent(ReviewRegistryEvent event) {
    replyEvents.add(event);
  }

  @Override
  public void publishLikesHistoryEvent(LikesRegistryEvent event) {
    likesEvents.add(event);
  }

  @Override
  public void publishPicksHistoryEvent(PicksRegistryEvent event) {
    picksEvents.add(event);
  }

  @Override
  public void publishRatingHistoryEvent(RatingRegistryEvent event) {
    ratingEvents.add(event);
  }
}
