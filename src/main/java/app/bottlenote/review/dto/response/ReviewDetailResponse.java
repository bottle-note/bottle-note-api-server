package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.vo.ReviewInfo;

import java.util.List;

public record ReviewDetailResponse(
	AlcoholInfo alcoholInfo,
	ReviewInfo reviewResponse,
	List<ReviewImageInfo> reviewImageList
) {
	public static ReviewDetailResponse create(AlcoholInfo alcoholInfo, ReviewInfo reviewResponse, List<ReviewImageInfo> reviewImageList) {
		if (reviewResponse == null) {
			return new ReviewDetailResponse(null, null, null);
		}
		return new ReviewDetailResponse(alcoholInfo, reviewResponse, reviewImageList);
	}
}
