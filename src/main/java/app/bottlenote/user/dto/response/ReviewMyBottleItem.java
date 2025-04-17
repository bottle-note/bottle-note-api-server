package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record ReviewMyBottleItem(
		BaseMyBottleInfo baseMyBottleInfo,
		Long reviewId,
		boolean isMyReview,
		LocalDateTime reviewModifyAt,
		String reviewContent,
		Set<String> reviewTastingTags,
		boolean isBestReview
) {
}
