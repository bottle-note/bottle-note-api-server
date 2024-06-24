package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewImageInfo(
	@NotNull
	Long order,
	@NotNull
	String viewUrl

) {

}
