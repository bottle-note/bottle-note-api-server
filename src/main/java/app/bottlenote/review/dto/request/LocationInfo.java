package app.bottlenote.review.dto.request;

public record LocationInfo(
	String zipCode,
	String address,
	String detailAddress) {

}
