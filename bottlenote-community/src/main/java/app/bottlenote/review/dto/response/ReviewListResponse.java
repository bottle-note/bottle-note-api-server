package app.bottlenote.review.dto.response;

import app.bottlenote.review.facade.payload.ReviewInfo;
import java.util.List;

public record ReviewListResponse(List<ReviewInfo> reviewList) {
  public static ReviewListResponse of(List<ReviewInfo> reviewList) {
    return new ReviewListResponse(reviewList);
  }
}
