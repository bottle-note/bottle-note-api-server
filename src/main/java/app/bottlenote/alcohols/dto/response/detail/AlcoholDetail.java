package app.bottlenote.alcohols.dto.response.detail;

public record AlcoholDetail(
	AlcoholDetailInfo alcohols,
	FriendsDetailInfo friendsInfo,
	ReviewsDetailInfo reviews
) {
	public static AlcoholDetail of(AlcoholDetailInfo alcohols, FriendsDetailInfo friendsInfo, ReviewsDetailInfo reviews) {
		return new AlcoholDetail(alcohols, friendsInfo, reviews);
	}
}
