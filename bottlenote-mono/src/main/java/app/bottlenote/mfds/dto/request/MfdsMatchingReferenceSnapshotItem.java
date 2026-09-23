package app.bottlenote.mfds.dto.request;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import java.util.List;

/** 실행에 사용한 비교 집합의 해시 입력. */
public record MfdsMatchingReferenceSnapshotItem(
    List<AlcoholMatchTargetItem> alcohols,
    List<DistilleryMatchTargetItem> distilleries,
    List<RegionMatchTargetItem> regions) {}
