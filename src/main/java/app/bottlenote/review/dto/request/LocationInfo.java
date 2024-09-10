package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record LocationInfo(

	String locationName,

	@NotNull(message = "STREET_ADDRESS_REQUIRED")
	String streetAddress,

	//TODO : Enum으로 관리 필요
	String category,

	String mapUrl,

	String latitude,

	String longitude
) {

}
