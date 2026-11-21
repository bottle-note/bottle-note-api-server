package app.bottlenote.user.repository;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
import org.springframework.data.domain.PageRequest;

public interface CustomFollowRepository {

  List<FriendItem> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest);

  KeysetPageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria);

  KeysetPageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria);
}
