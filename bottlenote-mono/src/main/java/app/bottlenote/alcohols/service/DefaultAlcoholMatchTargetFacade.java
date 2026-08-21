package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.facade.AlcoholMatchTargetFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.common.annotation.FacadeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@FacadeService
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAlcoholMatchTargetFacade implements AlcoholMatchTargetFacade {

  private final AlcoholQueryRepository alcoholQueryRepository;
  private final DistilleryRepository distilleryRepository;
  private final RegionRepository regionRepository;

  @Override
  public List<AlcoholMatchTargetItem> findAllAlcoholTargets() {
    return alcoholQueryRepository.findAllMatchTargets();
  }

  @Override
  public List<AlcoholMatchTargetItem> findAlcoholTargetsByIds(List<Long> alcoholIds) {
    if (alcoholIds == null || alcoholIds.isEmpty()) {
      return List.of();
    }
    return alcoholQueryRepository.findMatchTargetsByIdIn(alcoholIds);
  }

  @Override
  public List<DistilleryMatchTargetItem> findAllDistilleryTargets() {
    return distilleryRepository.findAllOrderBySortOrderAsc().stream()
        .map(
            distillery ->
                new DistilleryMatchTargetItem(
                    distillery.getId(), distillery.getKorName(), distillery.getEngName()))
        .toList();
  }

  @Override
  public List<RegionMatchTargetItem> findAllRegionTargets() {
    return regionRepository.findAllOrderBySortOrderAsc().stream()
        .map(
            region ->
                new RegionMatchTargetItem(region.getId(), region.getKorName(), region.getEngName()))
        .toList();
  }

  @Override
  public boolean existsAlcohol(Long alcoholId) {
    return Boolean.TRUE.equals(alcoholQueryRepository.existsByAlcoholId(alcoholId));
  }

  @Override
  public boolean existsDistillery(Long distilleryId) {
    return distilleryRepository.findById(distilleryId).isPresent();
  }

  @Override
  public boolean existsRegion(Long regionId) {
    return regionRepository.findById(regionId).isPresent();
  }
}
