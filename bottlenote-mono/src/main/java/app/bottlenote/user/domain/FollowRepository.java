package app.bottlenote.user.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@DomainRepository
public interface FollowRepository {

  Follow save(Follow follow);

  Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId);

  List<FriendItem> getTastingFriendsInfoList(
      Long alcoholId, Long userId, PageRequest pageRequest);

  PageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria);

  PageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria);
}
