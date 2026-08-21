package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.facade.AlcoholMatchTargetFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AlcoholMatchTargetFacade의 테스트 더블. unit 테스트에서 비교 대상 집합을 직접 구성한다. */
public class FakeAlcoholMatchTargetFacade implements AlcoholMatchTargetFacade {

  private final Map<Long, AlcoholMatchTargetItem> alcohols = new LinkedHashMap<>();
  private final Map<Long, DistilleryMatchTargetItem> distilleries = new LinkedHashMap<>();
  private final Map<Long, RegionMatchTargetItem> regions = new LinkedHashMap<>();

  public void addAlcohol(AlcoholMatchTargetItem item) {
    alcohols.put(item.alcoholId(), item);
  }

  public void addDistillery(DistilleryMatchTargetItem item) {
    distilleries.put(item.id(), item);
  }

  public void addRegion(RegionMatchTargetItem item) {
    regions.put(item.id(), item);
  }

  public void clear() {
    alcohols.clear();
    distilleries.clear();
    regions.clear();
  }

  @Override
  public List<AlcoholMatchTargetItem> findAllAlcoholTargets() {
    return List.copyOf(alcohols.values());
  }

  @Override
  public List<AlcoholMatchTargetItem> findAlcoholTargetsByIds(List<Long> alcoholIds) {
    return alcohols.values().stream()
        .filter(item -> alcoholIds.contains(item.alcoholId()))
        .toList();
  }

  @Override
  public List<DistilleryMatchTargetItem> findAllDistilleryTargets() {
    return List.copyOf(distilleries.values());
  }

  @Override
  public List<RegionMatchTargetItem> findAllRegionTargets() {
    return List.copyOf(regions.values());
  }

  @Override
  public boolean existsAlcohol(Long alcoholId) {
    return alcohols.containsKey(alcoholId);
  }

  @Override
  public boolean existsDistillery(Long distilleryId) {
    return distilleries.containsKey(distilleryId);
  }

  @Override
  public boolean existsRegion(Long regionId) {
    return regions.containsKey(regionId);
  }
}
