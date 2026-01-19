package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaRegionQueryRepository extends RegionRepository, CrudRepository<Region, Long> {

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.RegionsItem(r.id, r.korName, r.engName, r.description)
      from region r order by r.id asc
      """)
  List<RegionsItem> findAllRegionsResponse();

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminRegionItem(
        r.id, r.korName, r.engName, r.continent, r.description, r.createAt, r.lastModifyAt
      )
      from region r
      where (:keyword is null or :keyword = ''
        or r.korName like concat('%', :keyword, '%')
        or r.engName like concat('%', :keyword, '%'))
      """)
  Page<AdminRegionItem> findAllRegions(@Param("keyword") String keyword, Pageable pageable);
}
