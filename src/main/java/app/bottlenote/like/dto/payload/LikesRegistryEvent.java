package app.bottlenote.like.dto.payload;

public record LikesRegistryEvent(
	Long reviewId,
	Long userId
) {
	
	public static LikesRegistryEvent of(Long reviewId, Long userId) {
		return new LikesRegistryEvent(reviewId, userId);
	}
}
