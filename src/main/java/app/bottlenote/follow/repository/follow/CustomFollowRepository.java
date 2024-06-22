package app.bottlenote.follow.repository.follow;

import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.global.service.cursor.PageResponse;

public interface CustomFollowRepository {

	PageResponse<FollowSearchResponse> followList(FollowPageableCriteria criteria, Long userId);
}
