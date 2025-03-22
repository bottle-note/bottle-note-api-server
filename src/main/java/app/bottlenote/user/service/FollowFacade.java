package app.bottlenote.user.service;

import app.bottlenote.alcohols.facade.payload.FriendInfo;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface FollowFacade {

	List<FriendInfo> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest);
}
