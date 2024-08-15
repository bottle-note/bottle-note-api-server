package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewImageInfo(
	@NotNull(message = "VALUE_REQUIRED")
	Long order,
	@NotNull(message = "VALUE_REQUIRED")
	String viewUrl

) {

	public static ReviewImageInfo create(Long order, String viewUrl) {
		return new ReviewImageInfo(order, viewUrl);
	}

}
