package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.Pattern;

public record LocationInfo(
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

	public static LocationInfo empty() {
		return new LocationInfo(null, null, null, null, null, null, null, null);
	}
}
