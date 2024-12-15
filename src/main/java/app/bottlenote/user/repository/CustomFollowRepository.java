package app.bottlenote.user.repository;

import app.bottlenote.alcohols.dto.response.detail.FriendsDetailInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowSearchResponse;

public interface CustomFollowRepository {

	PageResponse<FollowSearchResponse> getRelationList(Long userId, FollowPageableCriteria criteria);

	FriendsDetailInfo getTastingFriendsInfoList(Long alcoholId, Long userId);
}
