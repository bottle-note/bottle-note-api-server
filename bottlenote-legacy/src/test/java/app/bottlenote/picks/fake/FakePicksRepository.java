package app.bottlenote.picks.fake;

import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.shared.picks.constant.PicksStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class FakePicksRepository implements PicksRepository {

  protected final Map<Long, Picks> picksDatabase = new HashMap<>();

  public FakePicksRepository() {
    for (long id = 0; id < 5; id++)
      picksDatabase.put(
          id, Picks.builder().id(id).alcoholId(id).userId(id).status(PicksStatus.PICK).build());
  }

  @Override
  public Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
    return picksDatabase.values().stream()
        .filter(picks -> picks.getAlcoholId().equals(alcoholId) && picks.getUserId().equals(userId))
        .findFirst();
  }

  @Override
  public Picks save(Picks picks) {
    long id = picksDatabase.size() + 1L;
    picksDatabase.put(id, picks);
    ReflectionTestUtils.setField(picks, "id", id);
    return picks;
  }
}
