package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlService.Decision;
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
    properties.setDefaultRateLimit(new RateLimitRule(3, 60));

    PathRateLimitRule authRule = new PathRateLimitRule();
    authRule.setPathPrefix("/api/v2/auth");
    authRule.setLimit(2);
    authRule.setWindowSeconds(60);
    properties.setPathRules(List.of(authRule));
    properties.setExcludedPathPrefixes(List.of("/actuator"));

    service = new AccessControlService(store, properties, AccessControlMetrics.noop());
  }

  @Test
  @DisplayName("기본 rate limit을 초과하면 RATE_LIMITED 이다")
  void evaluate_whenDefaultLimitExceeded_returnsRateLimited() {
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols").allowed()).isTrue();
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols").allowed()).isTrue();
    assertThat(service.evaluate("203.0.113.1", "/api/v1/alcohols").allowed()).isTrue();

    Decision denied = service.evaluate("203.0.113.1", "/api/v1/alcohols");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
  }

  @Test
  @DisplayName("path rule이 기본 한도보다 우선한다")
  void evaluate_whenPathRuleConfigured_usesTighterLimit() {
    assertThat(service.evaluate("198.51.100.2", "/api/v2/auth/kakao").allowed()).isTrue();
    assertThat(service.evaluate("198.51.100.2", "/api/v2/auth/kakao").allowed()).isTrue();

    Decision denied = service.evaluate("198.51.100.2", "/api/v2/auth/kakao");
    assertThat(denied.type()).isEqualTo(Decision.Type.RATE_LIMITED);
  }

  @Test
  @DisplayName("ban 된 IP는 BANNED 이다")
  void evaluate_whenBanned_returnsBanned() {
    service.banIp("203.0.113.9", Duration.ofMinutes(10), "abuse");

    Decision decision = service.evaluate("203.0.113.9", "/api/v1/alcohols");
    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
  }

  @Test
  @DisplayName("unban 하면 다시 허용된다")
  void evaluate_whenUnbanned_allowsAgain() {
    service.banIp("203.0.113.10", Duration.ofMinutes(10), "abuse");
    service.unbanIp("203.0.113.10");

    assertThat(service.evaluate("203.0.113.10", "/api/v1/alcohols").allowed()).isTrue();
  }

  @Test
  @DisplayName("excluded path는 rate limit을 적용하지 않는다")
  void evaluate_whenExcludedPath_skipsRateLimit() {
    for (int i = 0; i < 10; i++) {
      assertThat(service.evaluate("203.0.113.11", "/actuator/health").allowed()).isTrue();
    }
  }

  @Test
  @DisplayName("excluded path라도 ban 이면 거부한다")
  void evaluate_whenExcludedPathButBanned_returnsBanned() {
    service.banIp("203.0.113.12", Duration.ofMinutes(5), "abuse");

    Decision decision = service.evaluate("203.0.113.12", "/actuator/health");
    assertThat(decision.type()).isEqualTo(Decision.Type.BANNED);
  }

  @Test
  @DisplayName("fail-open 이 true 이면 store 예외 시 허용한다")
  void evaluate_whenStoreFailsAndFailOpen_allows() {
    AccessControlStore failingStore =
        new AccessControlStore() {
          @Override
          public boolean isBanned(String ip) {
            throw new IllegalStateException("redis down");
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
          public long tryConsume(String key, int limit, Duration window) {
            return 0;
          }
        };
    AccessControlService failingService =
        new AccessControlService(failingStore, properties, AccessControlMetrics.noop());

    assertThat(failingService.evaluate("203.0.113.13", "/api/v1/alcohols").allowed()).isTrue();
  }
}
