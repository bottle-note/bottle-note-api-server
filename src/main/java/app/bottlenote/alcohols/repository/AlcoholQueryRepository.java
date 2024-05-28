package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlcoholQueryRepository extends JpaRepository<Alcohol, Long>,
	CustomAlcoholQueryRepository {

	Optional<Alcohol> findAlcoholById(Long id);
}
