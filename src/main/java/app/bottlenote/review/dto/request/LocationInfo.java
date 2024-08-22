package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocationInfo(

	@Pattern(regexp = "\\d{5}", message = "ZIPCODE_ONLY_NUMBER")
	@Size(min = 5, max = 5, message = "ZIPCODE_FORMAT")
	String zipCode,

	String address,

	String detailAddress
) {

}
