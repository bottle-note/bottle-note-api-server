package app.bottlenote.review.dto.request;

import app.bottlenote.review.constant.BestReviewChangeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record AdminBestReviewSelectionLogSearchRequest(
    Long reviewId,
    Long alcoholId,
    BestReviewChangeType changeType,
    LocalDate selectedFrom,
    LocalDate selectedTo,
    @Min(value = 0, message = "ADMIN_BEST_REVIEW_LOG_PAGE_MINIMUM") Integer page,
    @Min(value = 1, message = "ADMIN_BEST_REVIEW_LOG_SIZE_MINIMUM")
        @Max(value = 100, message = "ADMIN_BEST_REVIEW_LOG_SIZE_MAXIMUM")
        Integer size) {

  public AdminBestReviewSelectionLogSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
