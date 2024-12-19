package app.bottlenote.picks.repository;

import app.bottlenote.picks.domain.Picks;

import java.util.Optional;

public interface PicksRepository {

	Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

	Picks save(Picks picks);
}
