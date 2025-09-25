package app.bottlenote.shared.review.payload;

public record ReviewReplyRegistryEvent(Long reviewId, Long userId, String content) {
  public static ReviewReplyRegistryEvent replyRegistryPublish(
      Long reviewId, Long userId, String content) {
    return new ReviewReplyRegistryEvent(reviewId, userId, content);
  }
}
