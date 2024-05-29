package app.bottlenote.rating.dto.request;

import app.bottlenote.rating.domain.RatingPoint;
import jakarta.validation.constraints.NotNull;

public record RatingRegisterRequest(
	@NotNull(message = "alcoholId는 필수값입니다.")
	Long alcoholId,
	@NotNull(message = "별점은 필수값입니다.")
	RatingPoint rating
) {
}
