package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewStatusChangeRequest(

	@NotNull(message = "REVIEW_DISPLAY_STATUS_NOT_EMPTY")
	ReviewDisplayStatus status

) {

}
