package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record LocationInfo(
	@NotEmpty
	String zipCode,
	@NotEmpty
	String address,
	@NotEmpty
	String detailAddress) {

	@Builder
	public LocationInfo {
	}
}
