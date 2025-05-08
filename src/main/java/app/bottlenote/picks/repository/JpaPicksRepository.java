package app.bottlenote.picks.repository;


import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.picks.domain.Picks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@JpaRepositoryImpl
public interface JpaPicksRepository extends PicksRepository, JpaRepository<Picks, Long> {
	Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
