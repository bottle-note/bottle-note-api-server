package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.vo.ReviewInfo;

import java.util.List;

public record ReviewDetailResponse(
	AlcoholInfo alcoholInfo,
	ReviewInfo reviewInfo,
	List<ReviewImageInfo> reviewImageList
) {
	public static ReviewDetailResponse create(AlcoholInfo alcoholInfo, ReviewInfo reviewInfo, List<ReviewImageInfo> reviewImageList) {
		if (reviewInfo == null) {
			return new ReviewDetailResponse(alcoholInfo, null, null);
		}
		return new ReviewDetailResponse(alcoholInfo, reviewInfo, reviewImageList);
	}
}
