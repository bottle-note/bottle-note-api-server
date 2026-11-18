package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
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
  public Optional<User> findByIdForUpdate(Long userId) {
    return findById(userId);
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
  public boolean existsByNickName(String nickname) {
    return users.values().stream().anyMatch(user -> user.getNickName().equals(nickname));
  }
}
