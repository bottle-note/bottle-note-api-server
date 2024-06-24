package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocationInfo(

	@Pattern(regexp = "\\d{5}", message = "우편번호는 숫자만 가능합니다.")
	@Size(min = 5, max = 5, message = "우편번호는 5자리의 숫자 형식입니다.")
	String zipCode,

	String address,

	String detailAddress
) {

}
