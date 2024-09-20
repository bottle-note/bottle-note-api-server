package app.bottlenote.review.domain.event;

public record ReviewRegistryEvent(
	Long reviewId,
	Long alcoholId,
	Long userId,
	String content
) {
	public static ReviewRegistryEvent reviewRegistryPublish(
		Long reviewId,
		Long alcoholId,
		Long userId,
		String content
	){
		return new ReviewRegistryEvent(reviewId,alcoholId, userId, content);
	}
}
