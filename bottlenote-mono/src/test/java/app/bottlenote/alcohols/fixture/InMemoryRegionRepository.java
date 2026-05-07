package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryRegionRepository implements RegionRepository {

  private final List<Region> regions = new ArrayList<>();
  private final Map<Long, Long> alcoholCountByRegion = new HashMap<>();

  @Override
  public Optional<Region> findById(Long id) {
    return regions.stream().filter(r -> Objects.equals(r.getId(), id)).findFirst();
  }

  @Override
  public List<RegionsItem> findAllRegionsResponse() {
    return regions.stream()
        .sorted(
            Comparator.comparingInt(Region::getSortOrder)
                .thenComparing(Region::getKorName, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(
            r ->
                RegionsItem.of(
                    r.getId(),
                    r.getKorName(),
                    r.getEngName(),
                    r.getDescription(),
                    r.getParent() != null ? r.getParent().getId() : null,
                    r.getSortOrder()))
        .toList();
  }

  @Override
  public Page<AdminRegionItem> findAllRegions(String keyword, Pageable pageable) {
    List<AdminRegionItem> filtered =
        regions.stream()
            .filter(
                r ->
                    keyword == null
                        || keyword.isEmpty()
                        || (r.getKorName() != null && r.getKorName().contains(keyword))
                        || (r.getEngName() != null && r.getEngName().contains(keyword)))
            .sorted(
                Comparator.comparingInt(Region::getSortOrder)
                    .thenComparing(
                        Region::getKorName, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(
                r ->
                    new AdminRegionItem(
                        r.getId(),
                        r.getKorName(),
                        r.getEngName(),
                        r.getContinent(),
                        r.getDescription(),
                        r.getCreateAt(),
                        r.getLastModifyAt(),
                        r.getParent() != null ? r.getParent().getId() : null,
                        r.getSortOrder()))
            .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AdminRegionItem> pageContent =
        start < filtered.size() ? filtered.subList(start, end) : List.of();
    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  @Override
  public List<Long> findChildRegionIds(Long parentId) {
    return regions.stream()
        .filter(r -> r.getParent() != null && Objects.equals(r.getParent().getId(), parentId))
        .map(Region::getId)
        .toList();
  }

  @Override
  public List<Long> findChildRegionIdsIn(Collection<Long> parentIds) {
    return regions.stream()
        .filter(r -> r.getParent() != null && parentIds.contains(r.getParent().getId()))
        .map(Region::getId)
        .toList();
  }

  @Override
  public Region save(Region region) {
    if (region.getId() == null) {
      Long newId = (long) (regions.size() + 1);
      ReflectionTestUtils.setField(region, "id", newId);
    }
    final Long rid = region.getId();
    regions.removeIf(r -> Objects.equals(r.getId(), rid));
    regions.add(region);
    return region;
  }

  @Override
  public void delete(Region region) {
    regions.removeIf(r -> Objects.equals(r.getId(), region.getId()));
  }

  @Override
  public boolean existsByKorName(String korName) {
    return regions.stream().anyMatch(r -> Objects.equals(r.getKorName(), korName));
  }

  @Override
  public boolean existsByEngName(String engName) {
    return regions.stream().anyMatch(r -> Objects.equals(r.getEngName(), engName));
  }

  @Override
  public boolean existsByKorNameAndIdNot(String korName, Long id) {
    return regions.stream()
        .anyMatch(r -> Objects.equals(r.getKorName(), korName) && !Objects.equals(r.getId(), id));
  }

  @Override
  public boolean existsByEngNameAndIdNot(String engName, Long id) {
    return regions.stream()
        .anyMatch(r -> Objects.equals(r.getEngName(), engName) && !Objects.equals(r.getId(), id));
  }

  @Override
  public List<Region> findAllBySortOrderGreaterThanEqual(int sortOrder) {
    return regions.stream().filter(r -> r.getSortOrder() >= sortOrder).toList();
  }

  @Override
  public boolean existsAlcoholByRegionId(Long regionId) {
    return alcoholCountByRegion.getOrDefault(regionId, 0L) > 0;
  }

  @Override
  public long countAlcoholsByRegionId(Long regionId) {
    return alcoholCountByRegion.getOrDefault(regionId, 0L);
  }

  public void setAlcoholCount(Long regionId, long count) {
    alcoholCountByRegion.put(regionId, count);
  }

  public void clear() {
    regions.clear();
    alcoholCountByRegion.clear();
  }
}
