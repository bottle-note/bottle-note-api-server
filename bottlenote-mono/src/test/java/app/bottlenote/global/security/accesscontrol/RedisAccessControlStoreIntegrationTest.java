package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.ConsumeResult;
import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@Testcontainers
@DisplayName("RedisAccessControlStore 통합 테스트")
class RedisAccessControlStoreIntegrationTest {

  @Container
  static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.0.12"));

  private StringRedisTemplate redisTemplate;
  private RedisAccessControlStore store;

  @BeforeEach
  void setUp() {
    redisTemplate = createTemplate(Duration.ofSeconds(2));
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    store = new RedisAccessControlStore(redisTemplate);
  }

  @Test
  @DisplayName("ban/getBan/unban이 Redis에 반영된다")
  void ban_getBan_unban_roundTrip() {
    store.ban("203.0.113.10", Duration.ofMinutes(5), "abuse");

    assertThat(store.isBanned("203.0.113.10")).isTrue();
    BanInfo ban = store.getBan("203.0.113.10");
    assertThat(ban).isNotNull();
    assertThat(ban.ip()).isEqualTo("203.0.113.10");
    assertThat(ban.reason()).isEqualTo("abuse");
    assertThat(ban.ttlSeconds()).isPositive();

    store.unban("203.0.113.10");
    assertThat(store.isBanned("203.0.113.10")).isFalse();
    assertThat(store.getBan("203.0.113.10")).isNull();
  }

  @Test
  @DisplayName("versioned projection은 역순 밴과 해제를 최신 이벤트로 보호한다")
  void projectBanAndUnban_whenDeliveredOutOfOrder_preservesLatestEvent() {
    String ip = "203.0.113.77";

    store.projectBan(ip, Duration.ofMinutes(5), "first", 10L);
    store.projectUnban(ip, 11L);
    store.projectBan(ip, Duration.ofMinutes(5), "late-first", 10L);

    assertThat(store.isBanned(ip)).isFalse();
    assertThat(store.getBan(ip)).isNull();

    store.projectBan(ip, Duration.ofMinutes(5), "second", 12L);
    store.projectUnban(ip, 11L);

    assertThat(store.isBanned(ip)).isTrue();
    assertThat(store.getBan(ip).reason()).isEqualTo("second");
  }

  @Test
  @DisplayName("배포 전 legacy ban 키도 enforcement·조회·목록에서 유지한다")
  void legacyBan_whenOnlyLegacyKeysExist_remainsEnforcedAndListed() {
    String ip = "203.0.113.78";
    redisTemplate.opsForValue().set("bn:ac:ban:" + ip, "1", Duration.ofMinutes(5));
    redisTemplate
        .opsForValue()
        .set("bn:ac:ban-reason:" + ip, "legacy-abuse", Duration.ofMinutes(5));

    assertThat(store.isBanned(ip)).isTrue();
    assertThat(store.getBan(ip).reason()).isEqualTo("legacy-abuse");
    assertThat(store.listBans(10)).extracting(BanInfo::ip).containsExactly(ip);
  }

  @Test
  @DisplayName("projection unban은 legacy stale key까지 제거하고 공존 목록을 중복하지 않는다")
  void projectUnban_whenLegacyAndProjectedKeysCoexist_removesLegacyAndDeduplicates() {
    String ip = "203.0.113.79";
    redisTemplate.opsForValue().set("bn:ac:ban:" + ip, "1", Duration.ofMinutes(5));
    redisTemplate
        .opsForValue()
        .set("bn:ac:ban-reason:" + ip, "legacy-abuse", Duration.ofMinutes(5));
    store.projectBan(ip, Duration.ofMinutes(5), "projected-abuse", 20L);

    assertThat(store.listBans(10))
        .singleElement()
        .satisfies(ban -> assertThat(ban.reason()).isEqualTo("projected-abuse"));

    store.projectUnban(ip, 19L);

    assertThat(store.isBanned(ip)).isTrue();
    assertThat(redisTemplate.hasKey("bn:ac:ban:" + ip)).isTrue();

    store.projectUnban(ip, 21L);

    assertThat(store.isBanned(ip)).isFalse();
    assertThat(redisTemplate.hasKey("bn:ac:ban:" + ip)).isFalse();
    assertThat(redisTemplate.hasKey("bn:ac:ban-reason:" + ip)).isFalse();
  }

  @Test
  @DisplayName("listBans는 활성 ban을 제한 개수까지 반환한다 (pipeline 조회)")
  void listBans_returnsActiveBansWithoutPerItemSequentialLookup() {
    IntStream.rangeClosed(1, 25)
        .forEach(i -> store.ban("203.0.113." + i, Duration.ofMinutes(10), "r-" + i));

    List<BanInfo> bans = store.listBans(20);

    assertThat(bans).hasSize(20);
    assertThat(bans)
        .allSatisfy(
            ban -> {
              assertThat(ban.ip()).startsWith("203.0.113.");
              assertThat(ban.reason()).startsWith("r-");
              assertThat(ban.ttlSeconds()).isNotEqualTo(-2L);
            });
    assertThat(store.listBans(500)).hasSize(25);
  }

  @Test
  @DisplayName("tryConsume은 fixed-window 한도를 적용한다")
  void tryConsume_enforcesLimit() {
    ConsumeResult first = store.tryConsume("product:1.2.3.4|default", 2, Duration.ofSeconds(60));
    ConsumeResult second = store.tryConsume("product:1.2.3.4|default", 2, Duration.ofSeconds(60));
    ConsumeResult third = store.tryConsume("product:1.2.3.4|default", 2, Duration.ofSeconds(60));

    assertThat(first.allowed()).isTrue();
    assertThat(second.allowed()).isTrue();
    assertThat(third.allowed()).isFalse();
    assertThat(third.retryAfterSeconds()).isPositive();
  }

  @Test
  @DisplayName("access-control commandTimeout(200ms)은 연결 불가 시 전역 15s보다 빨리 실패한다")
  void shortCommandTimeout_failsFasterThanGlobalDefault() {
    // TEST-NET-1 비라우팅 주소 — connectTimeout 200ms로 빠르게 실패해야 한다
    Duration shortTimeout = Duration.ofMillis(200);
    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder()
            .commandTimeout(shortTimeout)
            .clientOptions(
                ClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(shortTimeout).build())
                    .build())
            .build();
    RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration("192.0.2.1", 6399);
    LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
    factory.afterPropertiesSet();
    factory.start();
    StringRedisTemplate broken = new StringRedisTemplate(factory);
    broken.afterPropertiesSet();
    RedisAccessControlStore brokenStore = new RedisAccessControlStore(broken);

    long started = System.nanoTime();
    assertThatThrownBy(() -> brokenStore.isBanned("203.0.113.99"))
        .isInstanceOfAny(
            RedisCommandTimeoutException.class,
            org.springframework.dao.QueryTimeoutException.class,
            org.springframework.data.redis.RedisConnectionFailureException.class,
            org.springframework.dao.DataAccessResourceFailureException.class);
    long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

    factory.destroy();
    // 전역 15s를 쓰지 않음 — 여유 포함 2s 이내 실패
    assertThat(elapsedMs).isLessThan(2_000);
  }

  private static StringRedisTemplate createTemplate(Duration commandTimeout) {
    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .clientOptions(
                ClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(commandTimeout).build())
                    .build())
            .build();
    RedisStandaloneConfiguration standalone =
        new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort());
    LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
    factory.afterPropertiesSet();
    factory.start();
    StringRedisTemplate template = new StringRedisTemplate(factory);
    template.afterPropertiesSet();
    return template;
  }
}
