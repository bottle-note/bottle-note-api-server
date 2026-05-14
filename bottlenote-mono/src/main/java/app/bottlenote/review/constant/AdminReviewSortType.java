package app.bottlenote.review.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminReviewSortType {
  CREATED_AT("작성일"),
  REPLY_COUNT("댓글 수"),
  UPDATED_AT("수정일");

  private final String description;
}
