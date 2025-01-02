package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;

public interface CustomFollowRepository {

	PageResponse<FollowingSearchResponse> getFollowingList(Long userId, FollowPageableCriteria criteria);

	PageResponse<FollowerSearchResponse> getFollowerList(Long userId, FollowPageableCriteria criteria);
}
