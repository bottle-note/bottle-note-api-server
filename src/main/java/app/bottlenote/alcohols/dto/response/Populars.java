package app.bottlenote.alcohols.dto.response;

import lombok.Builder;

@Builder
public record Populars(
	Long alcoholId,
	String korName,
	String engName,
	Double rating,
	Long ratingCount,
	String korCategory,
	String engCategory,
	String imageUrl,
	Boolean isPicked
) {
}
