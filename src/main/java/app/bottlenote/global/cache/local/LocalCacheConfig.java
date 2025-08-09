package app.bottlenote.global.cache.local;

import static java.time.LocalDateTime.now;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableCaching
public class LocalCacheConfig {
  /**
   * CaffeineCache를 활용한 캐시 매니저 빈 등록입니다. LocalCacheType enum에 정의된 캐시 정보를 바탕으로 캐시를 생성합니다.
   *
   * @return the cache manager
   */
  @Bean
  public CacheManager cacheManager() {
    List<CaffeineCache> caches =
        Arrays.stream(LocalCacheType.values())
            .map(
                localCacheType ->
                    new CaffeineCache(
                        localCacheType.getCacheName(),
                        Caffeine.newBuilder()
                            .recordStats() // 통계 정보를 수집하도록 설정
                            .expireAfterWrite(
                                localCacheType.getSecsToExpireAfterWrite(),
                                TimeUnit.SECONDS) // 캐시 만료 시간 설정
                            .maximumSize(localCacheType.getEntryMaxSize()) // 캐시 최대 크기 설정
                            .build()))
            .toList();
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(caches);

    log.info("{} :: cacheManager : {}", now(), cacheManager);
    return cacheManager;
  }
}
