package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.MyPageResponse;

public class UserQueryFixture {

	public MyPageResponse getMyPageInfo() {

		return MyPageResponse.builder()
			.userId(1L)
			.nickName("nickname")
			.imageUrl("test.trl.com")
			.reviewCount(10L)
			.ratingCount(10L)
			.pickCount(10L)
			.followerCount(5L)
			.followingCount(3L)
			.isFollow(true)
			.isMyPage(true)
			.build();

	}

}
