package app.bottlenote.picks.domain;

import app.bottlenote.picks.constant.PicksStatus;
import java.util.Optional;

public interface PicksRepository {

  Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

  Long countByAlcoholIdAndStatus(Long alcoholId, PicksStatus status);

  Picks save(Picks picks);
}
