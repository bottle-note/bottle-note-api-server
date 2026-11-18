package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAlcoholViewCounter 단위 테스트")
class RedisAlcoholViewCounterTest {

  private static final String BUCKET_KEY = "popularity:view:2026082314";

  @Mock private StringRedisTemplate redisTemplate;

  private SimpleMeterRegistry meterRegistry;
  private RedisAlcoholViewCounter counter;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    Clock clock = Clock.fixed(Instant.parse("2026-08-23T05:37:00Z"), ZoneId.of("Asia/Seoul"));
    counter = new RedisAlcoholViewCounter(redisTemplate, clock, meterRegistry);
  }

  @Test
  @DisplayName("서울 시간 버킷 Hash의 주류 필드를 원자적으로 증가시키고 72시간 보존한다")
  void increment_increasesHourlyHashAndAppliesRetention() {
    // when
    counter.increment(42L);

    // then
    verify(redisTemplate)
        .execute(
            anyRedisScript(),
            eq(List.of(BUCKET_KEY)),
            eq("42"),
            eq(String.valueOf(RedisAlcoholViewCounter.RETENTION.toSeconds())));
  }

  @Test
  @DisplayName("Redis 증가가 실패해도 예외를 전파하거나 재시도하지 않고 실패 지표를 남긴다")
  void increment_whenRedisFails_recordsFailureWithoutPropagationOrRetry() {
    // given
    when(redisTemplate.execute(
            anyRedisScript(),
            eq(List.of(BUCKET_KEY)),
            eq("42"),
            eq(String.valueOf(RedisAlcoholViewCounter.RETENTION.toSeconds()))))
        .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

    // when & then
    assertThatCode(() -> counter.increment(42L)).doesNotThrowAnyException();
    verify(redisTemplate)
        .execute(
            anyRedisScript(),
            eq(List.of(BUCKET_KEY)),
            eq("42"),
            eq(String.valueOf(RedisAlcoholViewCounter.RETENTION.toSeconds())));
    verify(redisTemplate, never()).opsForHash();
    assertThat(meterRegistry.get("popularity_view_counter_failures_total").counter().count())
        .isEqualTo(1.0);
  }

  private static DefaultRedisScript<Long> anyRedisScript() {
    return any();
  }
}
