package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserQueryRepository implements UserQueryRepository {

	private final Map<Long, User> users = new HashMap<>();

	@Override
	public User save(User User) {
		return users.put(User.getId(), User);
	}

	@Override
	public Optional<User> findById(Long UserId) {
		return Optional.ofNullable(users.get(UserId));
	}

	@Override
	public List<User> findAll() {
		return users.values().stream().toList();
	}

	@Override
	public List<User> findAllByIdIn(List<Long> ids) {
		return users.values().stream().filter(User -> ids.contains(User.getId())).toList();
	}
}
