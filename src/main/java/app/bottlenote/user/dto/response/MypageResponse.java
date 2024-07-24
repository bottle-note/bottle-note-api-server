package app.bottlenote.user.dto.response;

public record MypageResponse(
	Long userId,
	String nickName,
	String imageUrl,
	Long FollowingCount,
	Long FollowerCount,
	Long isPickCount,
	Long reviewCount,
	Long ratingCount,
	Boolean isMyPage,
	Boolean isFollowing
) {

	public static MypageResponse of(Long userId, String nickName, String imageUrl, Long FollowingCount, Long FollowerCount, Long isPickCount, Long reviewCount, Long ratingCount, Boolean isMyPage, Boolean isFollowing) {
		return new MypageResponse(userId, nickName, imageUrl, FollowingCount, FollowerCount, isPickCount, reviewCount, ratingCount, isMyPage, isFollowing);
	}

}
