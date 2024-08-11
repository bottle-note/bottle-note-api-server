package app.bottlenote.user.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

import java.util.List;

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

	public MyBottleResponse getMyBottleResponse(Long userId, boolean isMyPage, List<MyBottleResponse.MyBottleInfo> myBottleList, CursorPageable cursorPageable) {
		return MyBottleResponse.builder()
			.userId(userId)
			.isMyPage(isMyPage)
			.totalCount((long) myBottleList.size())
			.myBottleList(myBottleList)
			.cursorPageable(cursorPageable)
			.build();
	}

}
