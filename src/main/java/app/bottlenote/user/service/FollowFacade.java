package app.bottlenote.user.service;

import app.bottlenote.alcohols.dto.response.detail.FriendsDetailInfo;

public interface FollowFacade {
	
	FriendsDetailInfo getTastingFriendsInfoList(Long alcoholId, Long userId);
}
