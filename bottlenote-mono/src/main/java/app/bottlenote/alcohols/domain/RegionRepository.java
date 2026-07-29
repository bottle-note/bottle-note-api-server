package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegionRepository {

  Optional<Region> findById(Long id);

  List<RegionsItem> findAllRegionsResponse();

  Page<AdminRegionItem> findAllRegions(String keyword, Pageable pageable);

  List<Long> findChildRegionIds(Long parentId);

  List<Long> findChildRegionIdsIn(Collection<Long> parentIds);

  Region save(Region region);

  void delete(Region region);

  boolean existsByKorName(String korName);

  boolean existsByEngName(String engName);

  boolean existsByKorNameAndIdNot(String korName, Long id);

  boolean existsByEngNameAndIdNot(String engName, Long id);

  List<Region> findAllBySortOrderGreaterThanEqual(int sortOrder);

  List<Region> findAllOrderBySortOrderAsc();

  List<Region> findAllByParentIdOrderBySortOrderAsc(Long parentId);

  boolean existsAlcoholByRegionId(Long regionId);

  long countAlcoholsByRegionId(Long regionId);
}
