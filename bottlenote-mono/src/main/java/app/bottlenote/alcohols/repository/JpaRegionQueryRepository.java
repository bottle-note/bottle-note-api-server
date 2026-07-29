package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.Collection;
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
      select new app.bottlenote.alcohols.dto.response.RegionsItem(r.id, r.korName, r.engName, r.description, r.imageUrl, r.parent.id, r.sortOrder)
      from region r order by r.sortOrder asc, r.korName asc
      """)
  List<RegionsItem> findAllRegionsResponse();

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminRegionItem(
        r.id, r.korName, r.engName, r.continent, r.description, r.imageUrl, r.createAt, r.lastModifyAt, r.parent.id, r.sortOrder
      )
      from region r
      where (:keyword is null or :keyword = ''
        or r.korName like concat('%', :keyword, '%')
        or r.engName like concat('%', :keyword, '%'))
      order by r.sortOrder asc, r.korName asc
      """)
  Page<AdminRegionItem> findAllRegions(@Param("keyword") String keyword, Pageable pageable);

  @Override
  @Query("select r.id from region r where r.parent.id = :parentId")
  List<Long> findChildRegionIds(@Param("parentId") Long parentId);

  @Override
  @Query("select r.id from region r where r.parent.id in :parentIds")
  List<Long> findChildRegionIdsIn(@Param("parentIds") Collection<Long> parentIds);

  @Override
  boolean existsByKorName(String korName);

  @Override
  boolean existsByEngName(String engName);

  @Override
  boolean existsByKorNameAndIdNot(String korName, Long id);

  @Override
  boolean existsByEngNameAndIdNot(String engName, Long id);

  @Override
  @Query("select r from region r where r.sortOrder >= :sortOrder")
  List<Region> findAllBySortOrderGreaterThanEqual(@Param("sortOrder") int sortOrder);

  @Override
  @Query("select r from region r order by r.sortOrder asc, r.id asc")
  List<Region> findAllOrderBySortOrderAsc();

  @Override
  @Query("select r from region r where r.parent.id = :parentId order by r.sortOrder asc, r.id asc")
  List<Region> findAllByParentIdOrderBySortOrderAsc(@Param("parentId") Long parentId);

  @Override
  @Query(
      "select case when count(a) > 0 then true else false end from alcohol a where a.region.id = :regionId")
  boolean existsAlcoholByRegionId(@Param("regionId") Long regionId);

  @Override
  @Query("select count(a) from alcohol a where a.region.id = :regionId")
  long countAlcoholsByRegionId(@Param("regionId") Long regionId);
}
