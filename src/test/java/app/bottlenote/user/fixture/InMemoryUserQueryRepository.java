package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserQueryRepository implements UserQueryRepository {

	private final Map<Long, User> users = new HashMap<>();

	@Override
	public User save(User user) {
		long id = users.size() + 1L;
		users.put(id, user);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
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

	@Override
	public Boolean existsByUserId(Long userId) {
		return null;
	}

	@Override
	public Long countByUsername(String userName) {
		return null;
	}

	@Override
	public MyPageResponse getMyPage(Long userId, Long currentUserId) {
		return null;
	}

	@Override
	public MyBottleResponse getMyBottle(MyBottlePageableCriteria criteria) {
		return null;
	}

}
