package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.repository.custom.CustomPopularQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularQueryRepository extends JpaRepository<Alcohol, Long>, CustomPopularQueryRepository {
}
