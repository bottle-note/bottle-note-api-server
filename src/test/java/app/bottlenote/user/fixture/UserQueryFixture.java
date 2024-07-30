package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.MyPageResponse;

public class UserQueryFixture {

	public MyPageResponse getMyPageInfo(Long userId, String nickName, String imageUrl, Long reviewCount, Long ratingCount, Long pickCount, Long followerCount, Long followingCount, boolean isFollow, boolean isMyPage) {

		return MyPageResponse.builder()
			.userId(userId)
			.nickName(nickName)
			.imageUrl(imageUrl)
			.reviewCount(reviewCount)
			.ratingCount(ratingCount)
			.pickCount(pickCount)
			.followerCount(followerCount)
			.followingCount(followingCount)
			.isFollow(isFollow)
			.isMyPage(isMyPage)
			.build();

	}
}
