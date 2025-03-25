package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.Pattern;

public record LocationInfoRequest(
		String locationName,
		@Pattern(regexp = "^\\d{5}$", message = "INVALID_ZIP_CODE_PATTERN")
		String zipCode,
		String address,
		String detailAddress,
		String category,
		String mapUrl,
		String latitude,
		String longitude
) {

	public static LocationInfoRequest empty() {
		return new LocationInfoRequest(null, null, null, null, null, null, null, null);
	}
}
