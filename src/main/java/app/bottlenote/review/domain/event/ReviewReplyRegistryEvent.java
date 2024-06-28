package app.bottlenote.review.domain.event;

public record ReviewReplyRegistryEvent(
	Long alcoholId,
	Long reviewId,
	Long userId,
	String content
) {
	public static ReviewReplyRegistryEvent replyRegistryPublish(
		Long alcoholId,
		Long reviewId,
		Long userId,
		String content
	) {
		return new ReviewReplyRegistryEvent(alcoholId, reviewId, userId, content);
	}
}
