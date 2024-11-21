package app.bottlenote.user.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

import java.time.LocalDateTime;
import java.util.List;

public class UserQueryFixture {

	public MyPageResponse getMyPageInfo() {

		return MyPageResponse.builder()
			.userId(1L)
			.nickName("nickname")
			.imageUrl("imageUrl")
			.reviewCount(10L)
			.ratingCount(20L)
			.pickCount(30L)
			.followerCount(40L)
			.followingCount(50L)
			.isFollow(false)
			.isMyPage(false)
			.build();

	}

	public MyBottleResponse getMyBottleResponse(Long userId, boolean isMyPage, CursorPageable cursorPageable) {

		LocalDateTime now = LocalDateTime.now();

		MyBottleResponse.MyBottleInfo bottleInfo_1 = new MyBottleResponse.MyBottleInfo(
			1L, "글렌피딕 12년", "Glenfiddich 12 Year Old", "싱글 몰트 위스키",
			"https://example.com/image1.jpg", true, 4.5, true,
			now, now, now, now
		);

		MyBottleResponse.MyBottleInfo bottleInfo_2 = new MyBottleResponse.MyBottleInfo(
			2L, "맥캘란 18년", "Macallan 18 Year Old", "싱글 몰트 위스키",
			"https://example.com/image2.jpg", false, 0.0, false,
			now, now, now, now
		);

		List<MyBottleResponse.MyBottleInfo> myBottleList = List.of(bottleInfo_1, bottleInfo_2);

		return MyBottleResponse.builder()
			.userId(userId)
			.isMyPage(isMyPage)
			.totalCount((long) myBottleList.size())
			.myBottleList(myBottleList)
			.cursorPageable(cursorPageable)
			.build();
	}

}
