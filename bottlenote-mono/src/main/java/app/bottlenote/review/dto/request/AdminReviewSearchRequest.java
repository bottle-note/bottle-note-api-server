package app.bottlenote.review.dto.request;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import java.time.LocalDateTime;

public record AdminReviewSearchRequest(
    Long alcoholId,
    Long userId,
    ReviewActiveStatus activeStatus,
    ReviewDisplayStatus displayStatus,
    String keyword,
    LocalDateTime createdFrom,
    LocalDateTime createdTo,
    Integer page,
    Integer size) {

  public AdminReviewSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
