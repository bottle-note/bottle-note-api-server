package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.ReviewReplyStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record SubReviewReplyResponse(List<Item> reviewReplies) {

  public static SubReviewReplyResponse of(List<Item> reviewReplays) {
    return new SubReviewReplyResponse(reviewReplays);
  }

  public record Item(
      Long userId,
      String imageUrl,
      String nickName,
      Long rootReviewId,
      Long parentReviewReplyId,
      String parentReviewReplyAuthor,
      Long reviewReplyId,
      String reviewReplyContent,
      ReviewReplyStatus status,
      LocalDateTime createAt) {
    @Builder
    public Item {
      parentReviewReplyAuthor = "@" + parentReviewReplyAuthor;
    }
  }
}
