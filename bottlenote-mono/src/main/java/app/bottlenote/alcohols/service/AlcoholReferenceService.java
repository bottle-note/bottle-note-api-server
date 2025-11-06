package app.bottlenote.alcohols.service;

import static java.time.LocalDateTime.now;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.CurationKeywordRepository;
import app.bottlenote.alcohols.dto.request.CurationKeywordSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.alcohols.repository.JpaRegionQueryRepository;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  private final CurationKeywordRepository curationKeywordRepository;

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

  @Transactional(readOnly = true)
  public CursorResponse<CurationKeywordResponse> searchCurationKeywords(
      CurationKeywordSearchRequest request) {
    return curationKeywordRepository.searchCurationKeywords(
        request.keyword(), request.alcoholId(), request.cursor(), request.pageSize().intValue());
  }

  @Transactional(readOnly = true)
  public CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Long pageSize) {
    return curationKeywordRepository.getCurationAlcohols(curationId, cursor, pageSize.intValue());
  }

  @Transactional(readOnly = true)
  public Optional<Set<Long>> getCurationAlcoholIds(String keyword) {
    return curationKeywordRepository.findAlcoholIdsByKeyword(keyword);
  }
}
