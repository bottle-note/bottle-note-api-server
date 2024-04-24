package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularQueryRepository extends CrudRepository<Alcohol, Long> {
}
