package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@Testcontainers
@DisplayName("RedisAlcoholViewCounter 통합 테스트")
class RedisAlcoholViewCounterIntegrationTest {

  private static final String BUCKET_KEY = "popularity:view:2026082314";

  @Container
  static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.0.12"));

  private LettuceConnectionFactory connectionFactory;
  private RedisTemplate<String, Object> redisTemplate;
  private RedisAlcoholViewCounter counter;

  @BeforeEach
  void setUp() {
    connectionFactory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort()));
    connectionFactory.afterPropertiesSet();
    connectionFactory.start();

    redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    redisTemplate.afterPropertiesSet();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

    Clock clock = Clock.fixed(Instant.parse("2026-08-23T05:37:00Z"), ZoneId.of("Asia/Seoul"));
    counter = new RedisAlcoholViewCounter(redisTemplate, clock, new SimpleMeterRegistry());
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @Test
  @DisplayName("증가와 최초 TTL 설정을 하나의 script로 적용한다")
  void increment_appliesHashCountAndInitialTtl() {
    counter.increment(42L);
    counter.increment(42L);

    Object count = redisTemplate.opsForHash().get(BUCKET_KEY, "42");
    Long ttl = redisTemplate.getExpire(BUCKET_KEY, TimeUnit.SECONDS);

    assertThat(count).isInstanceOf(Number.class);
    assertThat(((Number) count).longValue()).isEqualTo(2L);
    assertThat(ttl).isBetween(Duration.ofHours(71).toSeconds(), Duration.ofHours(72).toSeconds());
  }

  @Test
  @DisplayName("기존 TTL이 있으면 후속 증가가 만료 시각을 연장하지 않는다")
  void increment_whenTtlExists_doesNotExtendExpiry() {
    counter.increment(42L);
    redisTemplate.expire(BUCKET_KEY, Duration.ofMinutes(1));

    counter.increment(42L);

    assertThat(redisTemplate.getExpire(BUCKET_KEY, TimeUnit.SECONDS)).isBetween(1L, 60L);
  }
}
