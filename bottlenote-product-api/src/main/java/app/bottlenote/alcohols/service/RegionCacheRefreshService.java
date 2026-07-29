package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCacheRefreshService {

  private static final String REGION_CACHE_NAME = "local_cache_alcohol_region_information";

  private final RegionRepository regionRepository;
  private final CacheManager cacheManager;
  private List<RegionsItem> lastKnownRegions;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional(readOnly = true)
  public void initializeRevision() {
    refresh();
  }

  @Scheduled(cron = "${schedules.region.cache.refresh.cron:0 */5 * * * *}")
  @Transactional(readOnly = true)
  public synchronized void refresh() {
    List<RegionsItem> currentRegions = regionRepository.findAllRegionsResponse();
    if (lastKnownRegions != null && !lastKnownRegions.equals(currentRegions)) {
      Cache cache = cacheManager.getCache(REGION_CACHE_NAME);
      if (cache != null) {
        cache.clear();
        log.info("지역 캐시를 갱신했습니다.");
      }
    }
    lastKnownRegions = currentRegions;
  }
}
