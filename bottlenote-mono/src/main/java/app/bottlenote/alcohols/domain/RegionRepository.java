package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminRegionItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegionRepository {

  Optional<Region> findById(Long id);

  List<RegionsItem> findAllRegionsResponse();

  Page<AdminRegionItem> findAllRegions(String keyword, Pageable pageable);
}
