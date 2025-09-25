package app.bottlenote.user.facade;

import app.bottlenote.shared.users.payload.FriendItem;
import java.util.List;
import org.springframework.data.domain.PageRequest;

public interface FollowFacade {

  List<FriendItem> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest);
}
