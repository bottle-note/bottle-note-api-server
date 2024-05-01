package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.response.RegionsResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionQueryRepository extends CrudRepository<Region, Long> {
	@Query("select new app.bottlenote.alcohols" +
            ".dto.response.RegionsResponse(r.id, r.korName, r.engName, r.description) " +
            "from region r " +
            "order by r.id asc")
	List<RegionsResponse> findAllRegionsResponse();
}
