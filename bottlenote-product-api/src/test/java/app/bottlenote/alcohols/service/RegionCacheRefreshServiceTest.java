package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("[unit] RegionCacheRefreshService")
class RegionCacheRefreshServiceTest {

  private static final String REGION_CACHE_NAME = "local_cache_alcohol_region_information";

  private final InMemoryRegionRepository regionRepository = new InMemoryRegionRepository();
  private final CacheManager cacheManager = new ConcurrentMapCacheManager(REGION_CACHE_NAME);
  private RegionCacheRefreshService service;

  @BeforeEach
  void setUp() {
    service = new RegionCacheRefreshService(regionRepository, cacheManager);
  }

  @Test
  @DisplayName("지역의 최종 수정 시각이 변경되면 Product 지역 캐시를 비운다")
  void refresh_whenRegionLastModifyAtChanged_clearsRegionCache() {
    Region region = Region.builder().korName("스코틀랜드").engName("Scotland").build();
    regionRepository.save(region);
    service.refresh();

    Cache cache = cacheManager.getCache(REGION_CACHE_NAME);
    cache.put(SimpleKey.EMPTY, "cached-regions");
    ReflectionTestUtils.setField(region, "lastModifyAt", LocalDateTime.of(2026, 7, 29, 12, 5));

    service.refresh();

    assertThat(cache.get(SimpleKey.EMPTY)).isNull();
  }

  @Test
  @DisplayName("지역이 삭제되면 Product 지역 캐시를 비운다")
  void refresh_whenRegionDeleted_clearsRegionCache() {
    Region region = Region.builder().korName("스코틀랜드").engName("Scotland").build();
    regionRepository.save(region);
    service.refresh();

    Cache cache = cacheManager.getCache(REGION_CACHE_NAME);
    cache.put(SimpleKey.EMPTY, "cached-regions");
    regionRepository.delete(region);

    service.refresh();

    assertThat(cache.get(SimpleKey.EMPTY)).isNull();
  }
}
