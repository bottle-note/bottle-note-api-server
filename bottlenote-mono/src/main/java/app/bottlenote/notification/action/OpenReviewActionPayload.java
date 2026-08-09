package app.bottlenote.notification.action;

public record OpenReviewActionPayload(Long replyId) {

  public OpenReviewActionPayload {
    if (replyId == null || replyId <= 0) {
      throw new IllegalArgumentException("댓글 식별자는 양수여야 합니다.");
    }
  }
}
