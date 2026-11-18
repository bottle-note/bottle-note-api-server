package app.bottlenote.alcohols.facade;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import java.util.List;

/** 타 도메인의 매칭 작업이 alcohols 애그리거트를 비교 대상으로 조회할 때 쓰는 공개 계약. */
public interface AlcoholMatchTargetFacade {

  /** 삭제되지 않은 전체 알코올을 매칭 비교용 요약으로 조회한다. */
  List<AlcoholMatchTargetItem> findAllAlcoholTargets();

  /** 지정한 ID들의 알코올을 매칭 비교용 요약으로 조회한다. */
  List<AlcoholMatchTargetItem> findAlcoholTargetsByIds(List<Long> alcoholIds);

  /** 전체 증류소를 매칭 비교용 요약으로 조회한다. */
  List<DistilleryMatchTargetItem> findAllDistilleryTargets();

  /** 지정한 ID들의 증류소를 매칭 비교용 요약으로 조회한다. */
  List<DistilleryMatchTargetItem> findDistilleryTargetsByIds(List<Long> distilleryIds);

  /** 전체 지역을 매칭 비교용 요약으로 조회한다. */
  List<RegionMatchTargetItem> findAllRegionTargets();

  /** 지정한 ID들의 지역을 매칭 비교용 요약으로 조회한다. */
  List<RegionMatchTargetItem> findRegionTargetsByIds(List<Long> regionIds);

  boolean existsAlcohol(Long alcoholId);

  boolean existsDistillery(Long distilleryId);

  boolean existsRegion(Long regionId);
}
