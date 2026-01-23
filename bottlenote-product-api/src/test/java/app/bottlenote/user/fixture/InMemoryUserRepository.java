package app.bottlenote.user.fixture;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserRepository implements UserRepository {

  private final Map<Long, User> database = new HashMap<>();
  private long sequence = 1L;

  @Override
  public User save(User user) {
    Long id = user.getId();
    if (Objects.isNull(id)) {
      id = sequence++;
      ReflectionTestUtils.setField(user, "id", id);
    }
    database.put(id, user);
    return user;
  }

  @Override
  public Optional<User> findById(Long userId) {
    return Optional.ofNullable(database.get(userId));
  }

  @Override
  public List<User> findAll() {
    return List.copyOf(database.values());
  }

  @Override
  public boolean existsByUserId(Long userId) {
    return database.containsKey(userId);
  }

  @Override
  public boolean existsByNickName(String nickname) {
    return database.values().stream().anyMatch(u -> u.getNickName().equals(nickname));
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

  public void clear() {
    database.clear();
    sequence = 1L;
  }
}
