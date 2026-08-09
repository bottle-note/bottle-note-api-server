package app.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.security.accesscontrol.AccessControlService;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * access-control을 이 클래스에서만 활성화하고, 실제 Redis + Security filter chain을 검증한다. application-test.yml
 * 기본값(enabled=false)은 다른 통합 테스트에 유지된다.
 */
@Tag("integration")
@DisplayName("[integration] Product AccessControl Security chain")
@ActiveProfiles({"test", "access-control-it"})
class AccessControlSecurityIntegrationTest extends IntegrationTestSupport {

  private static final String PUBLIC_PATH = "/api/v1/alcohols/search";

  @Autowired private AccessControlService accessControlService;
  @Autowired private StringRedisTemplate stringRedisTemplate;

  @BeforeEach
  void cleanAccessControlRedisState() {
    Set<String> keys = stringRedisTemplate.keys("bn:ac:*");
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
    }
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

  /** 문서용 TEST-NET 대역에서 테스트마다 고유 IP를 생성한다. */
  private static String nextTestIp() {
    int host = Math.floorMod(UUID.randomUUID().hashCode(), 250) + 1;
    return "203.0.113." + host;
  }
}
