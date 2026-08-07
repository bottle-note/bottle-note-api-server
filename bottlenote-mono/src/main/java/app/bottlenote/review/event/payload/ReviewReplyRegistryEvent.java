package app.bottlenote.review.event.payload;

/**
 * 리뷰 댓글 등록 도메인 이벤트.
 *
 * @param reviewId 리뷰 식별자
 * @param reviewAuthorId 리뷰 작성자(알림 대상) 식별자
 * @param replyUserId 댓글 작성자 식별자
 * @param replyId 생성된 댓글 식별자
 * @param content 댓글 내용
 */
public record ReviewReplyRegistryEvent(
    Long reviewId, Long reviewAuthorId, Long replyUserId, Long replyId, String content) {

  public static ReviewReplyRegistryEvent of(
      Long reviewId, Long reviewAuthorId, Long replyUserId, Long replyId, String content) {
    return new ReviewReplyRegistryEvent(reviewId, reviewAuthorId, replyUserId, replyId, content);
  }

  /** 리뷰 작성자 본인 댓글 여부. */
  public boolean isSelfReply() {
    return reviewAuthorId != null && reviewAuthorId.equals(replyUserId);
  }
}
