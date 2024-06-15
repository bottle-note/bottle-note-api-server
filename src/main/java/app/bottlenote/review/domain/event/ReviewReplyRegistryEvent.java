package app.bottlenote.review.domain.event;

public record ReviewReplyRegistryEvent(
	Long alcoholId,
	Long reviewId,
	Long userId,
	String content,
	Long parentReplyId
) {
}
