package app.bottlenote.alcohols.dto.response.detail;

public record AlcoholDetail(
	AlcoholDetailInfo alcohols,
	FriendsDetailInfo friendsInfo,
	ReviewsDetailInfo reviewList
) {

	public static AlcoholDetail of(AlcoholDetailInfo alcohols, FriendsDetailInfo friendsInfo, ReviewsDetailInfo reviewList) {
		return new AlcoholDetail(alcohols, friendsInfo, reviewList);
	}
}
