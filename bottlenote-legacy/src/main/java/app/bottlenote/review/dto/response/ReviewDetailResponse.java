package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.shared.review.payload.ReviewInfo;
import java.util.List;

public record ReviewDetailResponse(
    AlcoholSummaryItem alcoholInfo,
    ReviewInfo reviewInfo,
    List<ReviewImageInfoRequest> reviewImageList) {
  public ReviewDetailResponse {
    reviewInfo = (reviewInfo != null) ? reviewInfo : ReviewInfo.builder().build();
    reviewImageList = (reviewImageList != null) ? reviewImageList : List.of();
  }

  public static ReviewDetailResponse create(
      AlcoholSummaryItem alcoholSummaryItem,
      ReviewInfo reviewInfo,
      List<ReviewImageInfoRequest> reviewImageList) {
    if (reviewInfo == null) {
      return new ReviewDetailResponse(alcoholSummaryItem, ReviewInfo.builder().build(), List.of());
    }
    return new ReviewDetailResponse(
        alcoholSummaryItem, reviewInfo, reviewImageList != null ? reviewImageList : List.of());
  }
}
