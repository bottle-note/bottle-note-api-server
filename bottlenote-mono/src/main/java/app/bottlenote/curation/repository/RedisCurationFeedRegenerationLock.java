package app.bottlenote.curation.repository;

import app.bottlenote.curation.domain.CurationFeedRegenerationLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCurationFeedRegenerationLock implements CurationFeedRegenerationLock {

  private static final String LOCK_KEY = "curation:feed-payload:regeneration:lock";
  // 재생성이 끝나면 해제하므로 TTL은 프로세스가 죽었을 때의 안전장치다.
  private static final Duration LOCK_TTL = Duration.ofMinutes(10);

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public boolean tryAcquire() {
    return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL));
  }

  @Override
  public void release() {
    redisTemplate.delete(LOCK_KEY);
  }
}
