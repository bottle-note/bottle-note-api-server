package app.bottlenote.follow.fixture;

import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.dto.response.FollowUpdateResponse;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.List;

public class FollowQueryFixture {

	public FollowUpdateRequest getFollowUpdateRequest() {
		return new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);
	}

	public FollowUpdateResponse getFollowUpdateResponse() {
		return FollowUpdateResponse.builder()
			.status(FollowStatus.FOLLOWING)
			.followUserId(1L)
			.nickName("nickName")
			.imageUrl("imageUrl")
			.build();
	}

	public PageResponse<FollowSearchResponse> getPageResponse() {
		List<FollowDetail> followDetails = List.of(
			FollowDetail.of(
				1L,
				1L,
				"nickName1",
				"imageUrl1",
				FollowStatus.FOLLOWING,
				10L,
				5L
			),
			FollowDetail.of(
				2L,
				1L,
				"nickName2",
				"imageUrl2",
				FollowStatus.FOLLOWING,
				20L,
				10L
			),
			FollowDetail.of(
				3L,
				1L,
				"nickName3",
				"imageUrl3",
				FollowStatus.FOLLOWING,
				30L,
				15L
			)
		);

		FollowSearchResponse followSearchResponse = FollowSearchResponse.of(5L, followDetails);

		return PageResponse.of(followSearchResponse, CursorPageable.builder()
			.cursor(0L)
			.pageSize(50L)
			.hasNext(false)
			.build());
	}
}
