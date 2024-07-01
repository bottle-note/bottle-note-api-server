package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewImageInfo(
	@NotNull
	Long order,
	@NotNull
	String viewUrl

) {

	public static ReviewImageInfo create(Long order, String viewUrl) {
		return new ReviewImageInfo(order, viewUrl);
	}

}
