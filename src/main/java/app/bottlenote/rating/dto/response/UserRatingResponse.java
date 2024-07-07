package app.bottlenote.rating.dto.response;

import app.bottlenote.rating.domain.RatingPoint;

public record UserRatingResponse(
	Double rating,
	Long alcoholId,
	Long userId
) {
	public static UserRatingResponse empty(Long alcoholId, Long userId) {
		return new UserRatingResponse(0.0, alcoholId, userId);
	}

	public static UserRatingResponse create(RatingPoint rating, Long alcoholId, Long userId) {
		return new UserRatingResponse(rating.getRating(), alcoholId, userId);
	}
}
