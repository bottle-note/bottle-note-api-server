package app.bottlenote.picks.repository;


import app.bottlenote.picks.domain.Picks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PicksRepository extends JpaRepository<Picks, Long> {
	Optional<Picks> findByAlcohol_IdAndUser_Id(Long id, Long id1);
}
