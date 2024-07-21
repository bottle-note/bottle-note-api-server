package app.bottlenote.like.dto.request;

import app.bottlenote.like.domain.LikeStatus;
import jakarta.validation.constraints.NotNull;

public record LikesUpdateRequest(
	@NotNull(message = "reviewId(식별자)는 필수입니다.")
	Long reviewId,

	@NotNull(message = "status는 필수입니다.")
	LikeStatus status
) {
}
