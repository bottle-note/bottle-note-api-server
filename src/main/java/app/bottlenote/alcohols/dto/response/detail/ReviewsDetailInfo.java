package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.review.dto.vo.CommonReviewInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
public class ReviewsDetailInfo {
	private Long totalReviewCount;
	private List<CommonReviewInfo> bestReviewInfos;
	private List<CommonReviewInfo> recentReviewInfos;

	@Builder
	public ReviewsDetailInfo(
		Long totalReviewCount,
		List<CommonReviewInfo> bestReviewInfos,
		List<CommonReviewInfo> recentReviewInfos
	) {
		this.totalReviewCount = totalReviewCount;
		this.bestReviewInfos = bestReviewInfos;
		this.recentReviewInfos = recentReviewInfos;
	}
}
