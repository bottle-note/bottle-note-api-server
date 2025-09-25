package app.bottlenote.alcohols.service;

import static java.time.LocalDateTime.now;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.repository.JpaRegionQueryRepository;
import app.bottlenote.shared.alcohols.constant.AlcoholType;
import app.bottlenote.shared.alcohols.dto.response.CategoryItem;
import app.bottlenote.shared.alcohols.dto.response.RegionsItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholReferenceService {
  private final JpaRegionQueryRepository regionQueryRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Cacheable(value = "local_cache_alcohol_region_information")
  @Transactional(readOnly = true)
  public List<RegionsItem> findAllRegion() {
    log.info("RegionService.findAll() called , {}", now());
    return regionQueryRepository.findAllRegionsResponse();
  }

  @Cacheable(value = "local_cache_alcohol_category_information")
  @Transactional(readOnly = true)
  public List<CategoryItem> getAlcoholCategory(AlcoholType type) {
    return alcoholQueryRepository.findAllCategories(type);
  }
}
