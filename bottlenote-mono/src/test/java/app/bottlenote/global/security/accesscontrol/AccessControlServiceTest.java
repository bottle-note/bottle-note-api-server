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
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AccessControlService 단위 테스트")
class AccessControlServiceTest {

  private InMemoryAccessControlStore store;
  private AccessControlProperties properties;
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

    service = new AccessControlService(store, properties, AccessControlMetrics.noop());
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
    AccessControlService lookupService =
        new AccessControlService(lookupStore, properties, AccessControlMetrics.noop());

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
    AccessControlService failingService =
        new AccessControlService(failingStore, properties, AccessControlMetrics.noop());

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
    AccessControlService failingService =
        new AccessControlService(failingStore, properties, AccessControlMetrics.noop());

    assertThatThrownBy(() -> failingService.evaluate("203.0.113.14", "/api/v1/alcohols", "GET"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("programming error");
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
