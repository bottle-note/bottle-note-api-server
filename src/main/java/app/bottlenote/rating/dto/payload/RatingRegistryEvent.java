package app.bottlenote.rating.dto.payload;

import app.bottlenote.rating.domain.RatingPoint;

public record RatingRegistryEvent(
	Long alcoholId,
	Long userId,
	RatingPoint rating
) {

	public static RatingRegistryEvent of(Long alcoholId, Long userId, RatingPoint rating) {
		return new RatingRegistryEvent(alcoholId, userId, rating);
	}

}
