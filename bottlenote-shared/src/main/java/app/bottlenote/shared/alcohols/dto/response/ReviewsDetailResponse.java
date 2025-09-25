package app.bottlenote.shared.alcohols.dto.response;

import app.bottlenote.shared.review.payload.ReviewInfo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewsDetailResponse {
  private Long totalReviewCount;
  private List<ReviewInfo> bestReviewInfos;
  private List<ReviewInfo> recentReviewInfos;

  @Builder
  public ReviewsDetailResponse(
      Long totalReviewCount, List<ReviewInfo> bestReviewInfos, List<ReviewInfo> recentReviewInfos) {
    this.totalReviewCount = totalReviewCount;
    this.bestReviewInfos = bestReviewInfos;
    this.recentReviewInfos = recentReviewInfos;
  }
}
