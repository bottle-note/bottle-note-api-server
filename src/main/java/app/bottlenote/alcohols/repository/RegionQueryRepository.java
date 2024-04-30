package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Region;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionQueryRepository extends CrudRepository<Region, Long> {
	@Override
	List<Region> findAll();
}
