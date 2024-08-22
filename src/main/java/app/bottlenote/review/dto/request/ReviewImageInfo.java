package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewImageInfo(
	@NotNull(message = "REVIEW_IMAGE_ORDER_REQUIRED")
	Long order,
	@NotNull(message = "REVIEW_IMAGE_URL_REQUIRED")
	String viewUrl

) {

	public static ReviewImageInfo create(Long order, String viewUrl) {
		return new ReviewImageInfo(order, viewUrl);
	}

}
