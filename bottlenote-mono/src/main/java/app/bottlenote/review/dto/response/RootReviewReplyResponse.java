package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.ReviewReplyStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record RootReviewReplyResponse(List<Item> reviewReplies) {
  public static RootReviewReplyResponse of(List<Item> reviewReplyList) {
    return new RootReviewReplyResponse(reviewReplyList);
  }

  @Builder
  public record Item(
      Long userId,
      String imageUrl,
      String nickName,
      Long reviewReplyId,
      String reviewReplyContent,
      Long subReplyCount,
      ReviewReplyStatus status,
      LocalDateTime createAt) {}
}
