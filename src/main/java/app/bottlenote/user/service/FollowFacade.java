package app.bottlenote.user.service;

import app.bottlenote.alcohols.dto.response.detail.FriendInfo;
import java.util.List;
import org.springframework.data.domain.PageRequest;

public interface FollowFacade {

	List<FriendInfo> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest);
}
