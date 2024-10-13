package app.bottlenote.review.dto.request;

public record LocationInfo(

	String locationName,

	String streetAddress,

	String category,

	String mapUrl,

	String latitude,

	String longitude
) {

}
