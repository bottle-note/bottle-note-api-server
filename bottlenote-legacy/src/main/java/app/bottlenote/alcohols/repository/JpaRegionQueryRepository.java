package app.bottlenote.alcohols.repository;

import app.bottlenote.core.alcohols.domain.Region;
import app.bottlenote.core.alcohols.repository.RegionQueryRepository;
import app.bottlenote.shared.alcohols.dto.response.RegionsItem;
import app.bottlenote.shared.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@JpaRepositoryImpl
public interface JpaRegionQueryRepository
    extends RegionQueryRepository, CrudRepository<Region, Long> {

  @Override
  @Query(
      """
			select new app.bottlenote.shared.alcohols.dto.response.RegionsItem(r.id, r.korName, r.engName, r.description)
			from region r order by r.id asc
			""")
  List<RegionsItem> findAllRegionsResponse();
}
