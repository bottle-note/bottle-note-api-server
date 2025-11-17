package app.bottlenote.picks.domain;

import java.util.Optional;

public interface PicksRepository {

  Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

  Picks save(Picks picks);
}
