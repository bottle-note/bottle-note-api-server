package app.bottlenote.picks.fake;

import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.response.AlcoholPicksCountResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  public Long countByAlcoholIdAndStatus(Long alcoholId, PicksStatus status) {
    return picksDatabase.values().stream()
        .filter(picks -> Objects.equals(picks.getAlcoholId(), alcoholId))
        .filter(picks -> picks.getStatus() == status)
        .count();
  }

  @Override
  public List<AlcoholPicksCountResponse> countByAlcoholIdsAndStatus(
      List<Long> alcoholIds, PicksStatus status) {
    return alcoholIds.stream()
        .map(
            alcoholId ->
                new AlcoholPicksCountResponse(
                    alcoholId, countByAlcoholIdAndStatus(alcoholId, status)))
        .filter(count -> count.totalPickCount() > 0)
        .toList();
  }

  @Override
  public Picks save(Picks picks) {
    long id = picksDatabase.size() + 1L;
    picksDatabase.put(id, picks);
    ReflectionTestUtils.setField(picks, "id", id);
    return picks;
  }
}
