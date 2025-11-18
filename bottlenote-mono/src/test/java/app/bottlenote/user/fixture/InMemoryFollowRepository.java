package app.bottlenote.user.fixture;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryFollowRepository implements FollowRepository {

  private final Map<Long, Follow> database = new HashMap<>();

  @Override
  public Follow save(Follow follow) {
    Long id = follow.getId();
    if (Objects.isNull(id)) {
      id = (long) (database.size() + 1);
      ReflectionTestUtils.setField(follow, "id", id);
    }
    database.put(id, follow);
    return follow;
  }

  @Override
  public Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId) {
    return database.values().stream()
        .filter(f -> f.getUserId().equals(userId) && f.getTargetUserId().equals(followUserId))
        .findFirst();
  }

  @Override
  public List<FriendItem> getTastingFriendsInfoList(
      Long alcoholId, Long userId, PageRequest pageRequest) {
    return List.of();
  }

  @Override
  public PageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria) {
    return null;
  }

  @Override
  public PageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria) {
    return null;
  }
}
