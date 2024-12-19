package app.bottlenote.picks.mock;

import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.repository.PicksRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakePicksRepository implements PicksRepository {

	private final Map<Long, Picks> picksDatabase = new HashMap<>();

	@Override
	public Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
		return Optional.of(Picks.builder()
			.alcoholId(alcoholId)
			.userId(userId)
			.build());
	}

	@Override
	public Picks save(Picks picks) {
		long id = picksDatabase.size() + 1L;
		picksDatabase.put(id, picks);
		ReflectionTestUtils.setField(picks, "id", id);
		return picks;
	}
}
