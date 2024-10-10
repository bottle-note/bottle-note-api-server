package app.bottlenote.review.dto.payload;

public record ReviewRegistryEvent(
	Long reviewId,
	Long alcoholId,
	Long userId,
	String content
) {
	public static ReviewRegistryEvent of(Long id, Long alcoholId, Long userId, String content) {
		return new ReviewRegistryEvent(id, alcoholId, userId, content);
	}
}
