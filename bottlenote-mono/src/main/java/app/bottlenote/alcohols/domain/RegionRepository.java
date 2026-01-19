package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import java.util.List;

public interface RegionRepository {

  List<RegionsItem> findAllRegionsResponse();

  List<AdminRegionItem> findAllRegions();
}
