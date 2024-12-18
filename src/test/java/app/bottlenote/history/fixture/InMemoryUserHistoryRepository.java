package app.bottlenote.history.fixture;

import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserHistoryRepository implements UserHistoryRepository {

	private final Map<Long, UserHistory> historyies = new HashMap<>();

	@Override
	public UserHistory save(UserHistory userHistory) {
		long id = historyies.size() + 1L;
		historyies.put(id, userHistory);
		ReflectionTestUtils.setField(userHistory, "id", id);
		return userHistory;
	}

	@Override
	public Optional<UserHistory> findById(Long id) {
		return Optional.ofNullable(historyies.get(id));
	}

	@Override
	public List<UserHistory> findAll() {
		return historyies.values().stream().toList();
	}

	@Override
	public void delete(UserHistory userHistory) {
		historyies.remove(userHistory.getId());
	}
}
