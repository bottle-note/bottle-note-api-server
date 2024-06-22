package app.bottlenote.follow.repository.follower;

import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.global.service.cursor.PageResponse;

public interface CustomFollowerRepository {
	PageResponse<FollowSearchResponse> followerList(FollowPageableCriteria criteria);
}
