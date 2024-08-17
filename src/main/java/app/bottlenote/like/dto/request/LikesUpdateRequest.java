package app.bottlenote.like.dto.request;

import app.bottlenote.like.domain.LikeStatus;
import jakarta.validation.constraints.NotNull;

public record LikesUpdateRequest(
	@NotNull(message = "REVIEW_NOT_EMPTY")
	Long reviewId,

	@NotNull(message = "REVIEW_DISPLAY_STATUS_NOT_EMPTY")
	LikeStatus status
) {
}
