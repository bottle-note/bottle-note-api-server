package app.bottlenote.user.repository;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;

public interface CustomFollowRepository {

  KeysetPageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria);

  KeysetPageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria);
}
