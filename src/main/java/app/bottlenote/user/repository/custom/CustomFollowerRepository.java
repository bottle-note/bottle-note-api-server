package app.bottlenote.user.repository.custom;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowSearchResponse;

public interface CustomFollowerRepository {
	PageResponse<FollowSearchResponse> followerList(FollowPageableCriteria criteria);
}
