package app.bottlenote.picks.repository;


import app.bottlenote.picks.domain.Picks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPicksRepository extends PicksRepository, JpaRepository<Picks, Long> {
	Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
