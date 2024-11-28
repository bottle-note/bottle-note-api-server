package app.bottlenote.follow.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.constant.FollowStatus;
import app.bottlenote.user.dto.response.FollowSearchResponse;
import app.bottlenote.user.dto.response.FollowerDetail;
import app.bottlenote.user.dto.response.FollowingDetail;

import java.util.List;

public class FollowQueryFixture {

	public PageResponse<FollowSearchResponse> getPageResponse() {
		List<FollowingDetail> followingDetails = List.of(
			FollowingDetail.of(
				1L,
				1L,
				"nickName1",
				"imageUrl1",
				FollowStatus.FOLLOWING,
				10L,
				5L
			)
		);
		List<FollowerDetail> followerDetails = List.of(
			FollowerDetail.of(
				1L,
				1L,
				"nickName1",
				"imageUrl1",
				FollowStatus.FOLLOWING,
				10L,
				5L
			)
		);

		FollowSearchResponse followSearchResponse = FollowSearchResponse.of(5L, followingDetails, followerDetails);

		return PageResponse.of(followSearchResponse, CursorPageable.builder()
			.cursor(0L)
			.pageSize(50L)
			.hasNext(false)
			.build());
	}
}
