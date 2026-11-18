package app.bottlenote.curation.repository;

import app.bottlenote.curation.domain.CurationFeedRegenerationLock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCurationFeedRegenerationLock implements CurationFeedRegenerationLock {

  private static final String LOCK_KEY = "curation:feed-payload:regeneration:lock";
  // 재생성이 끝나면 해제하므로 TTL은 프로세스가 죽었을 때의 안전장치다.
  private static final Duration LOCK_TTL = Duration.ofMinutes(10);

  private final RedisTemplate<String, Object> redisTemplate;

  // 획득할 때마다 다른 토큰을 남긴다. TTL이 만료돼 다른 인스턴스가 락을 잡았을 때 남의 락을 지우지 않기 위해서다.
  private final AtomicReference<String> ownedToken = new AtomicReference<>();

  @Override
  public boolean tryAcquire() {
    String candidate = UUID.randomUUID().toString();
    boolean acquired =
        Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, candidate, LOCK_TTL));
    if (acquired) {
      ownedToken.set(candidate);
    }
    return acquired;
  }

  @Override
  public void release() {
    String owned = ownedToken.getAndSet(null);
    if (owned == null) {
      return;
    }
    if (owned.equals(redisTemplate.opsForValue().get(LOCK_KEY))) {
      redisTemplate.delete(LOCK_KEY);
      return;
    }
    log.warn("재생성 락 소유권이 이미 넘어가 해제를 건너뜁니다. 남은 락은 TTL({})로 만료됩니다.", LOCK_TTL);
  }
}
