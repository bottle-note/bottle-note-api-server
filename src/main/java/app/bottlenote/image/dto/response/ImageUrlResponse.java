package app.bottlenote.image.dto.response;

import lombok.Builder;

@Builder
public record ImageUrlResponse(
	String url
) {

}
