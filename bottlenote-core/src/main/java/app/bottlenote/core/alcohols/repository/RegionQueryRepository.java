package app.bottlenote.core.alcohols.repository;

import app.bottlenote.core.alcohols.domain.Region;
import app.bottlenote.shared.alcohols.dto.response.RegionsItem;
import java.util.List;

/** 지역 조회 질의에 관한 애그리거트를 정의합니다. */
public interface RegionQueryRepository {

  Region save(Region region);

  List<RegionsItem> findAllRegionsResponse();
}
