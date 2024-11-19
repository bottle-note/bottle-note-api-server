package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.review.dto.response.ReviewListResponse;
import lombok.Builder;

@Builder
public record AlcoholDetail(
	AlcoholDetailInfo alcohols,
	FriendsDetailInfo friendsInfo,
	ReviewListResponse reviewInfo
) {
}
