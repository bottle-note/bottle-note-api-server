package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
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
  public Optional<User> findByIdForUpdate(Long userId) {
    return findById(userId);
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

  public void clear() {
    database.clear();
    sequence = 1L;
  }
}
