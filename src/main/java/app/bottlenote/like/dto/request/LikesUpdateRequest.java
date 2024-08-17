package app.bottlenote.like.dto.request;

import app.bottlenote.like.domain.LikeStatus;
import jakarta.validation.constraints.NotNull;

public record LikesUpdateRequest(
	@NotNull(message = "REVIEWID_ID_REQUIRED")
	Long reviewId,

	@NotNull(message = "STATUS_IS_REQUIRED")
	LikeStatus status
) {
}
