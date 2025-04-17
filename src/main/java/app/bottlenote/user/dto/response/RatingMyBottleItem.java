package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RatingMyBottleItem(
		BaseMyBottleInfo baseMyBottleInfo,
		Double myRatingPoint,
		Double averageRatingPoint,
		Long averageRatingCount,
		LocalDateTime ratingModifyAt
) {
}
