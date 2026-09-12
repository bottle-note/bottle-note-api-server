package app.bottlenote.review.event.payload;

/** 리뷰 등록 History 입력 페이로드. */
public record ReviewRegistryEvent(Long reviewId, Long alcoholId, Long userId, String content) {
  public static ReviewRegistryEvent of(Long id, Long alcoholId, Long userId, String content) {
    return new ReviewRegistryEvent(id, alcoholId, userId, content);
  }
}
