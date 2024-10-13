package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.PopularAlcohol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularAlcoholRepository extends JpaRepository<PopularAlcohol, Long> {
}
