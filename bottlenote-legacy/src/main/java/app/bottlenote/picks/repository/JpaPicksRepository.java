package app.bottlenote.picks.repository;

import app.bottlenote.picks.domain.Picks;
import app.bottlenote.shared.annotation.JpaRepositoryImpl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaPicksRepository extends PicksRepository, JpaRepository<Picks, Long> {
  Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
