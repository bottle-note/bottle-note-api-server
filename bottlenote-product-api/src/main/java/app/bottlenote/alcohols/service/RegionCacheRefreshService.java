package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.response.RegionCacheRevision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCacheRefreshService {

  private static final String REGION_CACHE_NAME = "local_cache_alcohol_region_information";

  private final RegionRepository regionRepository;
  private final CacheManager cacheManager;
  private RegionCacheRevision lastKnownRevision;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeRevision() {
    refresh();
  }

  @Scheduled(cron = "${schedules.region.cache.refresh.cron:0 */5 * * * *}")
  public synchronized void refresh() {
    RegionCacheRevision currentRevision = regionRepository.getCacheRevision();
    if (lastKnownRevision != null && !lastKnownRevision.equals(currentRevision)) {
      Cache cache = cacheManager.getCache(REGION_CACHE_NAME);
      if (cache != null) {
        cache.clear();
        log.info("지역 캐시를 갱신했습니다. revision: {} -> {}", lastKnownRevision, currentRevision);
      }
    }
    lastKnownRevision = currentRevision;
  }
}
