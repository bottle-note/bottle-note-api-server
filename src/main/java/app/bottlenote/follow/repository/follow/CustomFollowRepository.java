package app.bottlenote.follow.repository.follow;

import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.global.service.cursor.PageResponse;

public interface CustomFollowRepository {
	PageResponse<FollowSearchResponse> findFollowList(Long userId, FollowPageableRequest pageableRequest);
}
