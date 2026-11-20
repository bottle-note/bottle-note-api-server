package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * admin 체인과 product 체인이 각자의 access-control 설정으로 연결되었는지 확인한다.
 *
 * <p>한도 값은 {@link AdminApiIntegrationTestSupport}가 product 1000000, admin 기본 900000, admin 경로 규칙
 * 700000, admin unknown-IP 800000으로 나눠 둔다. 응답 헤더의 한도로 어느 설정이 적용됐는지 구분한다.
 */
@Tag("integration")
@DisplayName("[integration] admin 체인 접근 제어 연결")
class AdminApiAccessControlIntegrationTest extends AdminApiIntegrationTestSupport {

  private static final String RATE_KEY_PREFIX = "bn:ac:rl:";

  @Autowired private StringRedisTemplate redisTemplate;

  @ParameterizedTest
  @CsvSource({
    "GET, /admin/api/v1/users, 900000",
    "GET, /admin/%61pi/v1/users, 900000",
    "POST, /admin/api/v1/auth/login, 700000",
    "POST, /admin/%61pi/v1/auth/login, 700000",
    "GET, /api/v1/alcohols/categories, 1000000"
  })
  @DisplayName("요청 경로에 따라 admin 경로는 admin 설정을, product 경로는 product 설정을 쓴다")
  void 경로별로_체인의_접근_제어_설정을_쓴다(String method, String path, String expectedLimit) {
    MvcTestResult result = call(method, path, "203.0.113.20");

    assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo(expectedLimit);
  }

  @Test
  @DisplayName("정상 경로와 mount를 인코딩한 경로로 로그인할 때 admin 네임스페이스의 같은 버킷에서 센다")
  void 인코딩_변형도_같은_admin_버킷을_쓴다() {
    String ip = "203.0.113.21";

    MvcTestResult first = call("POST", "/admin/api/v1/auth/login", ip);
    MvcTestResult second = call("POST", "/admin/%61pi/v1/auth/login", ip);

    long remaining = Long.parseLong(first.getResponse().getHeader("X-RateLimit-Remaining"));
    assertThat(second.getResponse().getHeader("X-RateLimit-Remaining"))
        .isEqualTo(String.valueOf(remaining - 1));
    assertThat(redisTemplate.hasKey(RATE_KEY_PREFIX + "admin-absorb-it-admin:" + ip + "|/v1/auth"))
        .isTrue();
    assertThat(redisTemplate.hasKey(RATE_KEY_PREFIX + "admin-absorb-it:" + ip + "|default"))
        .isFalse();
  }

  @Test
  @DisplayName("mount를 인코딩한 관리 평면 경로를 호출할 때 admin 앱처럼 rate limit을 건너뛴다")
  void 인코딩_변형_관리_평면도_한도를_건너뛴다() {
    MvcTestResult normal = call("GET", "/admin/api/v1/access-control/ip-bans", "203.0.113.22");
    MvcTestResult encoded = call("GET", "/admin/%61pi/v1/access-control/ip-bans", "203.0.113.22");

    assertThat(normal.getResponse().getHeader("X-RateLimit-Limit")).isNull();
    assertThat(encoded.getResponse().getHeader("X-RateLimit-Limit")).isNull();
  }

  @Test
  @DisplayName("IP를 해석하지 못한 요청을 받을 때 admin 경로는 admin unknown-IP 한도와 admin 버킷을 쓴다")
  void IP를_해석하지_못하면_체인별_unknown_설정을_쓴다() {
    MvcTestResult admin =
        mockMvcTester
            .get()
            .uri(URI.create("/admin/%61pi/v1/users"))
            .with(
                request -> {
                  request.setRemoteAddr("unknown");
                  return request;
                })
            .exchange();

    assertThat(admin.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo("800000");
    assertThat(redisTemplate.hasKey(RATE_KEY_PREFIX + "admin-absorb-it-admin:_unknown|unknown"))
        .isTrue();
  }

  private MvcTestResult call(String method, String path, String ip) {
    if ("POST".equals(method)) {
      return mockMvcTester
          .post()
          .uri(URI.create(path))
          .header("X-Forwarded-For", ip)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
          .exchange();
    }
    return mockMvcTester.get().uri(URI.create(path)).header("X-Forwarded-For", ip).exchange();
  }
}
