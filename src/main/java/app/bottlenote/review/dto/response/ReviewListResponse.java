package app.bottlenote.review.dto.response;

import app.bottlenote.review.dto.vo.ReviewInfo;

import java.util.List;

public record ReviewListResponse(
	Long totalCount,
	List<ReviewInfo> reviewList
) {
	public static ReviewListResponse of(Long totalCount, List<ReviewInfo> reviewList) {
		return new ReviewListResponse(totalCount, reviewList);
	}
}
