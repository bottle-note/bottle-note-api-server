package app.bottlenote.review.dto.request;

import lombok.Builder;

public record LocationInfo(
	String zipCode,
	String address,
	String detailAddress) {

	@Builder
	public LocationInfo {
	}
}
