package app.bottlenote.like.event.payload;

public record LikesRegistryEvent(Long reviewId, Long userId, String content) {

  public static LikesRegistryEvent of(Long reviewId, Long userId, String content) {
    return new LikesRegistryEvent(reviewId, userId, content);
  }
}
