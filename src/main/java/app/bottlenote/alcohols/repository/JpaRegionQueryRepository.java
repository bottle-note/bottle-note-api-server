package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@JpaRepositoryImpl
public interface JpaRegionQueryRepository extends CrudRepository<Region, Long> {

	@Query("""
			select new app.bottlenote.alcohols.dto.response.RegionsItem(r.id, r.korName, r.engName, r.description)
			from region r order by r.id asc
			""")
	List<RegionsItem> findAllRegionsResponse();

}
