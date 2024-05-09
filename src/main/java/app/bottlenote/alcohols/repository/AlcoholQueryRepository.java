package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlcoholQueryRepository extends JpaRepository<Alcohol, Long>, CustomAlcoholQueryRepository {
}
