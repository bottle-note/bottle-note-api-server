package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlService.Decision;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanLookup;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.ConsumeResult;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AccessControlService 단위 테스트")
class AccessControlServiceTest {

  private InMemoryAccessControlStore store;
  private AccessControlProperties properties;
  private BanSnapshotHolder snapshotHolder;
  private Clock clock;
  private SimpleMeterRegistry meterRegistry;
  private AccessControlMetrics metrics;
  private AccessControlService service;

  @BeforeEach
  void setUp() {
    store = new InMemoryAccessControlStore();
    properties = new AccessControlProperties();
    properties.setEnabled(true);
    properties.setFailOpen(true);
    properties.setKeyNamespace("product");
    properties.setDefaultRateLimit(new RateLimitRule(3, 60));
    properties.setUnknownIpRateLimit(new RateLimitRule(2, 60));
    properties.setManagementPathPrefixes(List.of("/v1/access-control"));

    PathRateLimitRule authRule = new PathRateLimitRule();
    authRule.setPathPrefix("/api/v2/auth");
    authRule.setLimit(2);
    authRule.setWindowSeconds(60);

    PathRateLimitRule exploreRule = new PathRateLimitRule();
    exploreRule.setPathPrefix("/api/v1/reviews/explore");
    exploreRule.setLimit(10);
    exploreRule.setWindowSeconds(60);

    PathRateLimitRule reviewsGetRule = new PathRateLimitRule();
    reviewsGetRule.setPathPrefix("/api/v1/reviews");
    reviewsGetRule.setMethods(List.of("GET"));
    reviewsGetRule.setLimit(5);
    reviewsGetRule.setWindowSeconds(60);

    PathRateLimitRule reviewsWriteRule = new PathRateLimitRule();
    reviewsWriteRule.setPathPrefix("/api/v1/reviews");
    reviewsWriteRule.setMethods(List.of("POST", "PATCH", "DELETE"));
    reviewsWriteRule.setLimit(2);
    reviewsWriteRule.setWindowSeconds(60);

    properties.setPathRules(List.of(authRule, exploreRule, reviewsGetRule, reviewsWriteRule));
    properties.setExcludedPathPrefixes(List.of("/actuator"));

    snapshotHolder = new BanSnapshotHolder(properties.getSnapshot().getMaxEntries());
    clock = Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
    meterRegistry = new SimpleMeterRegistry();
    metrics = new AccessControlMetrics(meterRegistry);
    service = newService(store);
  }

  @Test
  @DisplayName("기본 rate limit을 초과하면 RATE_LIMITED 이다")
  void evaluate_whenDefaultLimitExceeded_returnsRateLimited() {
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols", "GET").allowed()).isTrue();
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols", "GET").allowed()).isTrue();
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols", "GET").allowed()).isTrue();

    Decision denied = service.evaluate("203.0.113.1", "/api/v1/alcohols", "GET");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
    assertThat(denied.remaining()).isZero();
    assertThat(denied.retryAfterSeconds()).isPositive();
  }

  @Test
  @DisplayName("path rule이 기본 한도보다 우선한다")
  void evaluate_whenPathRuleConfigured_usesTighterLimit() {
    assertThat(service.evaluate("198.51.100.2", "/api/v2/auth/kakao", "POST").allowed()).isTrue();
    assertThat(service.evaluate("198.51.100.2", "/api/v2/auth/kakao", "POST").allowed()).isTrue();

    Decision denied = service.evaluate("198.51.100.2", "/api/v2/auth/kakao", "POST");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
  }

  @Test
  @DisplayName("reviews/explore는 더 긴 prefix 한도를 쓴다")
  void evaluate_whenExplorePath_usesLongerPrefixRule() {
    for (int i = 0; i < 10; i++) {
      assertThat(service.evaluate("198.51.100.9", "/api/v1/reviews/explore", "GET").allowed())
          .isTrue();
    }
    // reviews 쓰기 한도(2)와 분리되어 explore 10까지 허용
    Decision denied = service.evaluate("198.51.100.9", "/api/v1/reviews/explore", "GET");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
  }

  @Test
  @DisplayName("review GET은 읽기 한도를 사용한다")
  void evaluate_whenReviewGet_usesReadLimit() {
    for (int i = 0; i < 5; i++) {
      Decision decision = service.evaluate("198.51.100.10", "/api/v1/reviews/1", "GET");
      assertThat(decision.allowed()).isTrue();
      assertThat(decision.limit()).isEqualTo(5);
    }

    Decision denied = service.evaluate("198.51.100.10", "/api/v1/reviews/1", "GET");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
    assertThat(denied.limit()).isEqualTo(5);
  }

  @Test
  @DisplayName("review POST는 쓰기 한도를 사용한다")
  void evaluate_whenReviewPost_usesWriteLimit() {
    assertThat(service.evaluate("198.51.100.11", "/api/v1/reviews", "POST").allowed()).isTrue();
    assertThat(service.evaluate("198.51.100.11", "/api/v1/reviews", "POST").allowed()).isTrue();

    Decision denied = service.evaluate("198.51.100.11", "/api/v1/reviews", "POST");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
    assertThat(denied.limit()).isEqualTo(2);
  }

  @Test
  @DisplayName("review GET과 쓰기 요청은 서로 다른 Redis 버킷을 소비한다")
  void evaluate_whenReviewGetAndWrite_useSeparateBuckets() {
    // 쓰기 한도(2)를 소진해도 읽기 버킷은 독립
    assertThat(service.evaluate("198.51.100.12", "/api/v1/reviews", "POST").allowed()).isTrue();
    assertThat(service.evaluate("198.51.100.12", "/api/v1/reviews", "PATCH").allowed()).isTrue();
    assertThat(service.evaluate("198.51.100.12", "/api/v1/reviews", "DELETE").type())
        .isEqualTo(Decision.Type.RATE_LIMITED);

    Decision getAllowed = service.evaluate("198.51.100.12", "/api/v1/reviews/1", "GET");
    assertThat(getAllowed.allowed()).isTrue();
    assertThat(getAllowed.limit()).isEqualTo(5);
  }

  @Test
  @DisplayName("ban 된 IP는 BANNED 이다")
  void evaluate_whenBanned_returnsBanned() {
    service.banIp("203.0.113.9", Duration.ofMinutes(10), "abuse");

    Decision decision = service.evaluate("203.0.113.9", "/api/v1/alcohols", "GET");
    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
    assertThat(decision.retryAfterSeconds()).isPositive();
  }

  @Test
  @DisplayName("ban 조회는 lookupBan 한 번으로 판정한다")
  void evaluate_whenBanned_usesSingleLookupBan() {
    LookupCountingStore lookupStore = new LookupCountingStore();
    lookupStore.ban("203.0.113.91", Duration.ofMinutes(10), "abuse");
    AccessControlService lookupService = newService(lookupStore);

    Decision decision = lookupService.evaluate("203.0.113.91", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
    assertThat(lookupStore.lookupCount()).isOne();
  }

  @Test
  @DisplayName("unban 하면 다시 허용된다")
  void evaluate_whenUnbanned_allowsAgain() {
    service.banIp("203.0.113.10", Duration.ofMinutes(10), "abuse");
    service.unbanIp("203.0.113.10");

    assertThat(service.evaluate("203.0.113.10", "/api/v1/alcohols", "GET").allowed()).isTrue();
  }

  @Test
  @DisplayName("excluded path는 rate limit을 적용하지 않는다")
  void evaluate_whenExcludedPath_skipsRateLimit() {
    for (int i = 0; i < 10; i++) {
      assertThat(service.evaluate("203.0.113.11", "/actuator/health", "GET").allowed()).isTrue();
    }
  }

  @Test
  @DisplayName("excluded path라도 ban 이면 거부한다")
  void evaluate_whenExcludedPathButBanned_returnsBanned() {
    service.banIp("203.0.113.12", Duration.ofMinutes(5), "abuse");

    Decision decision = service.evaluate("203.0.113.12", "/actuator/health", "GET");
    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
  }

  @Test
  @DisplayName("management path는 ban 되어도 허용한다")
  void evaluate_whenManagementPathAndBanned_allows() {
    service.banIp("203.0.113.20", Duration.ofMinutes(5), "abuse");

    assertThat(service.evaluate("203.0.113.20", "/v1/access-control/ip-bans", "GET").allowed())
        .isTrue();
  }

  @Test
  @DisplayName("null IP는 unknown 한도를 적용한다")
  void evaluate_whenNullIp_appliesUnknownLimit() {
    assertThat(service.evaluate(null, "/api/v1/alcohols", "GET").allowed()).isTrue();
    assertThat(service.evaluate(null, "/api/v1/alcohols", "GET").allowed()).isTrue();
    Decision denied = service.evaluate(null, "/api/v1/alcohols", "GET");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
  }

  @Test
  @DisplayName("listBans는 활성 ban을 반환한다")
  void listBans_returnsActiveBans() {
    service.banIp("203.0.113.30", Duration.ofMinutes(5), "a");
    service.banIp("203.0.113.31", Duration.ofMinutes(5), "b");

    List<BanInfo> bans = service.listBans(10);
    assertThat(bans).hasSize(2);
    assertThat(bans)
        .extracting(BanInfo::ip)
        .containsExactlyInAnyOrder("203.0.113.30", "203.0.113.31");
  }

  @Test
  @DisplayName("fail-open 이 true 이면 store 예외 시 허용한다")
  void evaluate_whenStoreFailsAndFailOpen_allows() {
    AccessControlStore failingStore =
        new AccessControlStore() {
          @Override
          public BanLookup lookupBan(String ip) {
            throw new AccessControlStoreUnavailableException(
                new IllegalStateException("redis down"));
          }

          @Override
          public boolean isBanned(String ip) {
            return false;
          }

          @Override
          public void ban(String ip, Duration ttl, String reason) {}

          @Override
          public void unban(String ip) {}

          @Override
          public BanInfo getBan(String ip) {
            return null;
          }

          @Override
          public List<BanInfo> listBans(int max) {
            return List.of();
          }

          @Override
          public ConsumeResult tryConsume(String key, int limit, Duration window) {
            return ConsumeResult.allow(0);
          }
        };
    AccessControlService failingService = newService(failingStore);

    assertThat(failingService.evaluate("203.0.113.13", "/api/v1/alcohols", "GET").allowed())
        .isTrue();
  }

  @Test
  @DisplayName("저장소 기술 장애가 아닌 예외는 fail-open 하지 않는다")
  void evaluate_whenStoreProgrammingError_propagates() {
    AccessControlStore failingStore =
        new AccessControlStore() {
          @Override
          public BanLookup lookupBan(String ip) {
            throw new IllegalStateException("programming error");
          }

          @Override
          public boolean isBanned(String ip) {
            return false;
          }

          @Override
          public void ban(String ip, Duration ttl, String reason) {}

          @Override
          public void unban(String ip) {}

          @Override
          public BanInfo getBan(String ip) {
            return null;
          }

          @Override
          public List<BanInfo> listBans(int max) {
            return List.of();
          }

          @Override
          public ConsumeResult tryConsume(String key, int limit, Duration window) {
            return ConsumeResult.allow(0);
          }
        };
    AccessControlService failingService = newService(failingStore);

    assertThatThrownBy(() -> failingService.evaluate("203.0.113.14", "/api/v1/alcohols", "GET"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("programming error");
  }

  @Test
  @DisplayName("ban Redis 장애에서 fresh snapshot hit은 403을 반환하고 rate-limit을 호출하지 않는다")
  void evaluate_whenBanLookupFailsAndFreshSnapshotHits_returnsBannedWithoutRateLimitCall() {
    String ip = "203.0.113.40";
    snapshotHolder.replace(
        new BanSnapshot(
            Map.of(ip, new BanSnapshot.BanEntry(clock.instant().plusSeconds(120), "abuse")),
            clock.instant()));
    FailingBanStore failingStore = new FailingBanStore();

    Decision decision = newService(failingStore).evaluate(ip, "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
    assertThat(decision.retryAfterSeconds()).isEqualTo(120);
    assertThat(failingStore.rateLimitCalls()).isZero();
    assertCounter("access_control_ban_fallback_total", "snapshot_hit").isEqualTo(1.0);
    assertCounter("access_control_store_errors_total", null).isEqualTo(1.0);
  }

  @Test
  @DisplayName("무기한 snapshot ban은 60초 Retry-After를 반환한다")
  void evaluate_whenBanLookupFailsAndSnapshotIsIndefinite_returnsSixtySecondRetryAfter() {
    String ip = "203.0.113.41";
    snapshotHolder.replace(
        new BanSnapshot(
            Map.of(ip, new BanSnapshot.BanEntry(Instant.MAX, "abuse")), clock.instant()));

    Decision decision = newService(new FailingBanStore()).evaluate(ip, "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
    assertThat(decision.retryAfterSeconds()).isEqualTo(60);
  }

  @Test
  @DisplayName("ban Redis 장애에서 fresh snapshot miss는 fail-open allow하고 rate-limit을 호출하지 않는다")
  void evaluate_whenBanLookupFailsAndSnapshotMiss_allowsWithoutRateLimitCall() {
    snapshotHolder.replace(new BanSnapshot(Map.of(), clock.instant()));
    FailingBanStore failingStore = new FailingBanStore();

    Decision decision =
        newService(failingStore).evaluate("203.0.113.42", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.ALLOW);
    assertThat(failingStore.rateLimitCalls()).isZero();
    assertCounter("access_control_ban_fallback_total", "snapshot_miss").isEqualTo(1.0);
    assertCounter("access_control_fail_open_total", null).isEqualTo(1.0);
  }

  @Test
  @DisplayName("ban Redis 장애에서 만료된 snapshot entry는 fail-open 한다")
  void evaluate_whenBanLookupFailsAndSnapshotEntryExpired_allows() {
    String ip = "203.0.113.43";
    snapshotHolder.replace(
        new BanSnapshot(
            Map.of(ip, new BanSnapshot.BanEntry(clock.instant().minusSeconds(1), "expired")),
            clock.instant()));

    Decision decision = newService(new FailingBanStore()).evaluate(ip, "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.ALLOW);
    assertCounter("access_control_ban_fallback_total", "snapshot_miss").isEqualTo(1.0);
  }

  @Test
  @DisplayName("ban Redis 장애에서 stale snapshot은 entry가 있어도 fail-open 한다")
  void evaluate_whenBanLookupFailsAndSnapshotIsStale_allows() {
    String ip = "203.0.113.44";
    snapshotHolder.replace(
        new BanSnapshot(
            Map.of(ip, new BanSnapshot.BanEntry(clock.instant().plusSeconds(120), "abuse")),
            clock.instant().minus(Duration.ofMinutes(3)).minusNanos(1)));

    Decision decision = newService(new FailingBanStore()).evaluate(ip, "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.ALLOW);
    assertCounter("access_control_ban_fallback_total", "snapshot_stale").isEqualTo(1.0);
  }

  @Test
  @DisplayName("ban Redis 장애에서 startup empty snapshot은 fail-open 한다")
  void evaluate_whenBanLookupFailsAndSnapshotIsEmptyAtStartup_allows() {
    Decision decision =
        newService(new FailingBanStore()).evaluate("203.0.113.45", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.ALLOW);
    assertCounter("access_control_ban_fallback_total", "snapshot_stale").isEqualTo(1.0);
  }

  @Test
  @DisplayName("ban Redis 장애에서 fail-open false는 즉시 rate-limited 하고 rate-limit을 호출하지 않는다")
  void evaluate_whenBanLookupFailsAndFailOpenFalse_returnsRateLimitedWithoutRateLimitCall() {
    properties.setFailOpen(false);
    FailingBanStore failingStore = new FailingBanStore();

    Decision decision =
        newService(failingStore).evaluate("203.0.113.46", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.RATE_LIMITED);
    assertThat(decision.retryAfterSeconds()).isEqualTo(60);
    assertThat(failingStore.rateLimitCalls()).isZero();
  }

  @Test
  @DisplayName("rate-limit Redis 장애는 fail-open 메트릭과 함께 allow 한다")
  void evaluate_whenRateLimitFailsAndFailOpen_allowsAndRecordsMetric() {
    Decision decision =
        newService(new FailingRateLimitStore()).evaluate("203.0.113.47", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.ALLOW);
    assertCounter("access_control_rate_limit_fallback_total", "fail_open").isEqualTo(1.0);
    assertCounter("access_control_store_errors_total", null).isEqualTo(1.0);
  }

  @Test
  @DisplayName("rate-limit Redis 장애는 fail-open false에서 rate-limited 메트릭을 기록한다")
  void evaluate_whenRateLimitFailsAndFailOpenFalse_rateLimitsAndRecordsMetric() {
    properties.setFailOpen(false);

    Decision decision =
        newService(new FailingRateLimitStore()).evaluate("203.0.113.48", "/api/v1/alcohols", "GET");

    assertThat(decision.type()).isEqualTo(Decision.Type.RATE_LIMITED);
    assertCounter("access_control_rate_limit_fallback_total", "fail_closed").isEqualTo(1.0);
  }

  private AccessControlService newService(AccessControlStore accessControlStore) {
    return new AccessControlService(accessControlStore, properties, metrics, snapshotHolder, clock);
  }

  private org.assertj.core.api.AbstractDoubleAssert<?> assertCounter(String name, String result) {
    io.micrometer.core.instrument.search.Search search = meterRegistry.find(name);
    if (result != null) {
      search = search.tag("result", result);
    }
    return assertThat(search.counter().count());
  }

  private static final class FailingBanStore extends InMemoryAccessControlStore {

    private int rateLimitCalls;

    @Override
    public BanLookup lookupBan(String ip) {
      throw new AccessControlStoreUnavailableException(new IllegalStateException("redis down"));
    }

    @Override
    public ConsumeResult tryConsume(String key, int limit, Duration window) {
      rateLimitCalls++;
      throw new AssertionError("ban lookup failure must not call rate-limit Redis");
    }

    int rateLimitCalls() {
      return rateLimitCalls;
    }
  }

  private static final class FailingRateLimitStore extends InMemoryAccessControlStore {

    @Override
    public ConsumeResult tryConsume(String key, int limit, Duration window) {
      throw new AccessControlStoreUnavailableException(new IllegalStateException("redis down"));
    }
  }

  private static final class LookupCountingStore extends InMemoryAccessControlStore {

    private int lookupCount;

    @Override
    public BanLookup lookupBan(String ip) {
      lookupCount++;
      return super.isBanned(ip) ? new BanLookup(true, 60) : BanLookup.notBanned();
    }

    @Override
    public boolean isBanned(String ip) {
      throw new AssertionError("evaluate must use lookupBan");
    }

    @Override
    public BanInfo getBan(String ip) {
      throw new AssertionError("evaluate must use lookupBan");
    }

    int lookupCount() {
      return lookupCount;
    }
  }
}
