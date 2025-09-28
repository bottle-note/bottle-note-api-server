package app.bottlenote.rating.event.payload;

import app.bottlenote.rating.domain.RatingPoint;

public record RatingRegistryEvent(
    Long alcoholId, Long userId, RatingPoint prevRating, RatingPoint currentRating) {

  public static RatingRegistryEvent of(
      Long alcoholId, Long userId, RatingPoint prevRating, RatingPoint currentRating) {
    return new RatingRegistryEvent(alcoholId, userId, prevRating, currentRating);
  }
}
