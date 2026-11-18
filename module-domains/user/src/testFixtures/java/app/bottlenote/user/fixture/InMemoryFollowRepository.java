package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
}
