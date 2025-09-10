package app.bottlenote.user.fixture;

import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserQueryRepository implements UserRepository {

  private final Map<Long, User> users = new HashMap<>();

  @Override
  public User save(User user) {
    long id = users.size() + 1L;
    users.put(id, user);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  @Override
  public Optional<User> findById(Long userId) {
    return Optional.ofNullable(users.get(userId));
  }

  @Override
  public List<User> findAll() {
    return users.values().stream().toList();
  }

  @Override
  public boolean existsByUserId(Long userId) {
    return users.containsKey(userId);
  }

  @Override
  public MyPageResponse getMyPage(Long userId, Long currentUserId) {
    return null;
  }

  @Override
  public PageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria criteria) {
    return null;
  }

  @Override
  public PageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria criteria) {
    return null;
  }

  @Override
  public PageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria criteria) {
    return null;
  }

  @Override
  public boolean existsByNickName(String nickname) {
    return users.values().stream().anyMatch(user -> user.getNickName().equals(nickname));
  }
}
