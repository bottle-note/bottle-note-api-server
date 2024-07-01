package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import java.util.List;

public record ReviewDetailResponse(
	AlcoholInfo alcoholInfo,

	ReviewResponse reviewResponse,

	List<ReviewImageInfo> reviewImageList,

	List<ReviewReplyInfo> reviewReplyList

) {

	public static ReviewDetailResponse create(AlcoholInfo alcoholInfo, ReviewResponse reviewResponse, List<ReviewImageInfo> reviewImageList, List<ReviewReplyInfo> reviewReplyList) {
		return new ReviewDetailResponse(alcoholInfo, reviewResponse, reviewImageList, reviewReplyList);
	}
}

