package app.bottlenote.global.security.jwt;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.constant.MaliciousPathPattern;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.global.security.policy.SecurityPolicyRoute;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Tag("unit")
@DisplayName("[unit] admin JWT 필터의 악성 경로 판정 기준")
class AdminJwtAuthenticationFilterPolicyTest {

  private static final String ADMIN_API = "/admin/api";

  private final SecurityPolicyRegistry policyRegistry =
      new SecurityPolicyRegistry(
          List.of(
              SecurityPolicyRoute.explicit("POST", "/admin/api/v1/auth/login", PUBLIC),
              SecurityPolicyRoute.explicit("POST", "/admin/api/v1/auth/refresh", PUBLIC),
              SecurityPolicyRoute.explicit("GET", "/admin/api/v1/users", REQUIRED_AUTH)));

  private final TestAdminJwtAuthenticationFilter mountedFilter =
      new TestAdminJwtAuthenticationFilter(
          policyRegistry, request -> MaliciousPathPattern.matchesUnder(request, ADMIN_API));

  @ParameterizedTest
  @CsvSource({"POST, /admin/api/v1/auth/login", "POST, /admin/api/v1/auth/refresh"})
  @DisplayName("mount 기준을 받으면 /admin/api 아래 공개 경로는 기존 admin 앱처럼 JWT 필터를 건너뛴다")
  void shouldNotFilter_whenPublicAdminRouteUnderMount_returnsTrue(String method, String path) {
    assertThat(mountedFilter.shouldNotFilter(request(method, path))).isTrue();
  }

  @Test
  @DisplayName("mount 기준을 받으면 인증 필수 admin 경로는 JWT 필터를 실행한다")
  void shouldNotFilter_whenRequiredAdminRoute_returnsFalse() {
    assertThat(mountedFilter.shouldNotFilter(request("GET", "/admin/api/v1/users"))).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"GET, /admin/api/.env", "GET, /admin/api/admin/login.php"})
  @DisplayName("mount 안쪽의 스캐너 경로는 미수집 경로여도 JWT 필터를 실행한다")
  void shouldNotFilter_whenScannerPathUnderMount_returnsFalse(String method, String path) {
    assertThat(mountedFilter.shouldNotFilter(request(method, path))).isFalse();
  }

  @Test
  @DisplayName("판정 기준을 받지 않는 기존 생성자는 전체 경로 기준(MaliciousPathPattern.matches)을 쓴다")
  void defaultConstructor_usesFullPathMatcher() {
    TestAdminJwtAuthenticationFilter defaultFilter =
        new TestAdminJwtAuthenticationFilter(policyRegistry);

    // context-path 없이 /admin/api에 둔 공개 경로도 전체 경로 기준에서는 /admin/** 스캐너 경로라 필터를 건너뛰지 않는다
    assertThat(defaultFilter.shouldNotFilter(request("POST", "/admin/api/v1/auth/login")))
        .isFalse();
  }

  private static HttpServletRequest request(String method, String path) {
    return new MockHttpServletRequest(method, path);
  }

  private static class TestAdminJwtAuthenticationFilter extends AdminJwtAuthenticationFilter {
    TestAdminJwtAuthenticationFilter(
        SecurityPolicyRegistry policyRegistry, RequestMatcher maliciousPathMatcher) {
      super(null, policyRegistry, maliciousPathMatcher);
    }

    TestAdminJwtAuthenticationFilter(SecurityPolicyRegistry policyRegistry) {
      super(null, policyRegistry);
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
      return super.shouldNotFilter(request);
    }
  }
}
