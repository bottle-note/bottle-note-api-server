package app.bottlenote.user.facade;

import app.bottlenote.user.facade.payload.FriendItem;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface FollowFacade {

	List<FriendItem> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest);
}
