package app.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.security.accesscontrol.AccessControlService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.UnavailableException;
import app.bottlenote.global.security.accesscontrol.BanSnapshotRefresher;
import app.bottlenote.global.security.accesscontrol.RedisAccessControlStore;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * access-control을 이 클래스에서만 활성화하고, 실제 Redis + Security filter chain을 검증한다. application-test.yml
 * 기본값(enabled=false)은 다른 통합 테스트에 유지된다.
 */
@Tag("integration")
@DisplayName("[integration] Product AccessControl Security chain")
@ActiveProfiles({"test", "access-control-it"})
@TestPropertySource(properties = "bottlenote.access-control.burst-admission.cooldown=0s")
@Import(AccessControlSecurityIntegrationTest.AccessControlStoreTestConfiguration.class)
class AccessControlSecurityIntegrationTest extends IntegrationTestSupport {

  private static final String PUBLIC_PATH = "/api/v1/alcohols/search";

  @Autowired private AccessControlService accessControlService;
  @Autowired private StringRedisTemplate stringRedisTemplate;
  @Autowired private ControllableAccessControlStore accessControlStore;
  @Autowired private BanSnapshotRefresher banSnapshotRefresher;

  @BeforeEach
  void cleanAccessControlRedisState() {
    Set<String> keys = stringRedisTemplate.keys("bn:ac:*");
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
    }
    accessControlStore.reset();
    banSnapshotRefresher.refresh();
  }

  @Test
  @DisplayName("허용 IP 요청은 Security chain을 통과한다")
  void 허용_IP_요청_통과() {
    String clientIp = nextTestIp();

    var result =
        mockMvcTester.get().uri(PUBLIC_PATH).header("X-Forwarded-For", clientIp).exchange();

    result.assertThat().hasStatusOk();
    assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo("3");
  }

  @Test
  @DisplayName("ban된 IP 요청은 403을 반환한다")
  void ban된_IP_요청_403() {
    String clientIp = nextTestIp();
    accessControlService.banIp(clientIp, Duration.ofMinutes(5), "integration-ban");

    var result =
        mockMvcTester.get().uri(PUBLIC_PATH).header("X-Forwarded-For", clientIp).exchange();

    result.assertThat().hasStatus(FORBIDDEN);
    assertThat(result.getResponse().getHeader("Retry-After")).isNotBlank();
  }

  @Test
  @DisplayName("default rate limit을 초과하면 429를 반환한다")
  void rate_limit_초과_429() {
    String clientIp = nextTestIp();

    for (int i = 0; i < 3; i++) {
      mockMvcTester
          .get()
          .uri(PUBLIC_PATH)
          .header("X-Forwarded-For", clientIp)
          .exchange()
          .assertThat()
          .hasStatusOk();
    }

    var limited =
        mockMvcTester.get().uri(PUBLIC_PATH).header("X-Forwarded-For", clientIp).exchange();

    limited.assertThat().hasStatus(TOO_MANY_REQUESTS);
    assertThat(limited.getResponse().getHeader("Retry-After")).isNotBlank();
    assertThat(limited.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");
  }

  @Test
  @DisplayName("Redis 장애에도 fresh snapshot의 ban은 403을 반환한다")
  void Redis_장애_snapshot_ban_403() {
    String clientIp = nextTestIp();
    accessControlService.banIp(clientIp, Duration.ofMinutes(5), "snapshot-ban");
    banSnapshotRefresher.refresh();
    accessControlStore.failRequests();

    var result =
        mockMvcTester.get().uri(PUBLIC_PATH).header("X-Forwarded-For", clientIp).exchange();

    result.assertThat().hasStatus(FORBIDDEN);
    assertThat(accessControlStore.getTryConsumeCalls()).isZero();
  }

  @Test
  @DisplayName("Redis 복구 후 정상 allow와 rate limit 헤더를 반환한다")
  void Redis_복구_정상_allow_및_rate_limit_헤더() {
    String clientIp = nextTestIp();
    accessControlStore.failRequests();

    mockMvcTester
        .get()
        .uri(PUBLIC_PATH)
        .header("X-Forwarded-For", clientIp)
        .exchange()
        .assertThat()
        .hasStatusOk();
    assertThat(accessControlStore.getTryConsumeCalls()).isZero();

    accessControlStore.recover();
    var recovered =
        mockMvcTester.get().uri(PUBLIC_PATH).header("X-Forwarded-For", clientIp).exchange();

    recovered.assertThat().hasStatusOk();
    assertThat(recovered.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(recovered.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("2");
    assertThat(accessControlStore.getTryConsumeCalls()).isEqualTo(1);
  }

  /** 문서용 TEST-NET 대역에서 테스트마다 고유 IP를 생성한다. */
  private static String nextTestIp() {
    int host = Math.floorMod(UUID.randomUUID().hashCode(), 250) + 1;
    return "203.0.113." + host;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class AccessControlStoreTestConfiguration {

    @Bean
    @Primary
    ControllableAccessControlStore accessControlStore(StringRedisTemplate stringRedisTemplate) {
      return new ControllableAccessControlStore(new RedisAccessControlStore(stringRedisTemplate));
    }
  }

  static class ControllableAccessControlStore implements AccessControlStore {

    private final RedisAccessControlStore delegate;
    private final AtomicBoolean unavailable = new AtomicBoolean(false);
    private final AtomicInteger tryConsumeCalls = new AtomicInteger();

    ControllableAccessControlStore(RedisAccessControlStore delegate) {
      this.delegate = delegate;
    }

    void reset() {
      unavailable.set(false);
      tryConsumeCalls.set(0);
    }

    void failRequests() {
      unavailable.set(true);
    }

    void recover() {
      unavailable.set(false);
    }

    int getTryConsumeCalls() {
      return tryConsumeCalls.get();
    }

    @Override
    public BanLookup lookupBan(String ip) {
      failWhenUnavailable();
      return delegate.lookupBan(ip);
    }

    @Override
    public boolean isBanned(String ip) {
      return delegate.isBanned(ip);
    }

    @Override
    public void ban(String ip, Duration ttl, String reason) {
      delegate.ban(ip, ttl, reason);
    }

    @Override
    public void unban(String ip) {
      delegate.unban(ip);
    }

    @Override
    public void projectBan(String ip, Duration ttl, String reason, long eventId) {
      delegate.projectBan(ip, ttl, reason, eventId);
    }

    @Override
    public void projectUnban(String ip, long eventId) {
      delegate.projectUnban(ip, eventId);
    }

    @Override
    public void removeUnversionedBan(String ip) {
      delegate.removeUnversionedBan(ip);
    }

    @Override
    public BanInfo getBan(String ip) {
      return delegate.getBan(ip);
    }

    @Override
    public List<BanInfo> listBans(int max) {
      failWhenUnavailable();
      return delegate.listBans(max);
    }

    @Override
    public List<BanInfo> listUnversionedBans(int max) {
      return delegate.listUnversionedBans(max);
    }

    @Override
    public ConsumeResult tryConsume(String key, int limit, Duration window) {
      tryConsumeCalls.incrementAndGet();
      failWhenUnavailable();
      return delegate.tryConsume(key, limit, window);
    }

    private void failWhenUnavailable() {
      if (unavailable.get()) {
        throw new UnavailableException(new IllegalStateException("test-controlled Redis outage"));
      }
    }
  }
}
