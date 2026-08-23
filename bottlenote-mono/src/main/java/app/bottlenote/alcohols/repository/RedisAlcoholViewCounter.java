package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholViewCounter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class RedisAlcoholViewCounter implements AlcoholViewCounter {

  static final Duration RETENTION = Duration.ofHours(72);
  private static final ZoneId BUCKET_ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter BUCKET_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHH");
  private static final String KEY_PREFIX = "popularity:view:";
  private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>();

  static {
    INCREMENT_SCRIPT.setResultType(Long.class);
    INCREMENT_SCRIPT.setScriptText(
        """
        local current = redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
        if redis.call('TTL', KEYS[1]) < 0 then
          redis.call('EXPIRE', KEYS[1], ARGV[2])
        end
        return current
        """);
  }

  private final StringRedisTemplate redisTemplate;
  private final Clock clock;
  private final Counter failureCounter;

  @Autowired
  public RedisAlcoholViewCounter(
      StringRedisTemplate redisTemplate, ObjectProvider<MeterRegistry> meterRegistry) {
    this(
        redisTemplate,
        Clock.system(BUCKET_ZONE),
        meterRegistry.getIfAvailable(SimpleMeterRegistry::new));
  }

  RedisAlcoholViewCounter(
      StringRedisTemplate redisTemplate, Clock clock, MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.clock = clock;
    this.failureCounter =
        Counter.builder("popularity_view_counter_failures_total")
            .description("Failures while incrementing hourly alcohol view counters")
            .register(meterRegistry);
  }

  @Override
  public void increment(Long alcoholId) {
    String key = keyOf(LocalDateTime.now(clock));
    try {
      redisTemplate.execute(
          INCREMENT_SCRIPT,
          List.of(key),
          String.valueOf(alcoholId),
          String.valueOf(RETENTION.toSeconds()));
    } catch (RuntimeException exception) {
      failureCounter.increment();
      log.warn(
          "인기도 조회 카운터 증가 실패. key={}, alcoholId={}, error={}",
          key,
          alcoholId,
          exception.getClass().getSimpleName());
    }
  }

  @Override
  public Map<Long, Long> findCounts(LocalDateTime bucketAt) {
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(keyOf(bucketAt));
    Map<Long, Long> counts = new LinkedHashMap<>(entries.size());
    entries.forEach((alcoholId, count) -> counts.put(parseLong(alcoholId), parseLong(count)));
    return Map.copyOf(counts);
  }

  private static String keyOf(LocalDateTime bucketAt) {
    return KEY_PREFIX + BUCKET_FORMATTER.format(bucketAt);
  }

  private static long parseLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException exception) {
      throw new IllegalStateException("Redis 인기도 조회 카운터 값이 숫자가 아닙니다.", exception);
    }
  }
}
