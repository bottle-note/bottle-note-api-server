package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.ReviewReplyStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record SubReviewReplyResponse(Long totalCount, List<Item> reviewReplies) {

  public static SubReviewReplyResponse of(Long totalCount, List<Item> reviewReplays) {
    return new SubReviewReplyResponse(totalCount, reviewReplays);
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
