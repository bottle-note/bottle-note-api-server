package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.review.dto.vo.ReviewInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
public class ReviewsDetailInfo {
	private Long totalReviewCount;
	private List<ReviewInfo> bestReviewInfos;
	private List<ReviewInfo> recentReviewInfos;

	@Builder
	public ReviewsDetailInfo(
		Long totalReviewCount,
		List<ReviewInfo> bestReviewInfos,
		List<ReviewInfo> recentReviewInfos
	) {
		this.totalReviewCount = totalReviewCount;
		this.bestReviewInfos = bestReviewInfos;
		this.recentReviewInfos = recentReviewInfos;
	}
}
