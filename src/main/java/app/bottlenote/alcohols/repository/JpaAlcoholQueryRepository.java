package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAlcoholQueryRepository extends
	AlcoholQueryRepository,
	JpaRepository<Alcohol, Long>,
	CustomAlcoholQueryRepository {
}
