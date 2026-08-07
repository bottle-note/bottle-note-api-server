package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("unit")
@DisplayName("AccessControlFilter 단위 테스트")
class AccessControlFilterTest {

  private AccessControlService service;
  private AccessControlFilter filter;

  @BeforeEach
  void setUp() {
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    AccessControlProperties properties = new AccessControlProperties();
    properties.setDefaultRateLimit(new RateLimitRule(2, 60));
    PathRateLimitRuleSupport.authRule(properties);
    service = new AccessControlService(store, properties, AccessControlMetrics.noop());
    // Spring 운영 ObjectMapper와 동일하게 JavaTime 모듈 등록
    filter = new AccessControlFilter(service, new ObjectMapper().findAndRegisterModules());
  }

  @Test
  @DisplayName("허용 시 체인을 진행한다")
  void doFilter_whenAllowed_continuesChain() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols");
    request.addHeader("X-Forwarded-For", "203.0.113.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean continued = new AtomicBoolean(false);

    filter.doFilter(request, response, trackingChain(continued));

    assertThat(continued).isTrue();
    assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
    assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("2");
  }

  @Test
  @DisplayName("rate limit 초과 시 429를 반환한다")
  void doFilter_whenRateLimited_returns429() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols");
    request.addHeader("X-Forwarded-For", "203.0.113.2");
    AtomicBoolean continued = new AtomicBoolean(false);
    FilterChain chain = trackingChain(continued);

    filter.doFilter(request, new MockHttpServletResponse(), chain);
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    MockHttpServletResponse denied = new MockHttpServletResponse();
    filter.doFilter(request, denied, chain);

    assertThat(denied.getStatus()).isEqualTo(429);
    assertThat(denied.getHeader("Retry-After")).isNotBlank();
    assertThat(denied.getHeader("X-RateLimit-Limit")).isEqualTo("2");
    assertThat(denied.getContentAsString()).contains("요청이 너무 많습니다");
    assertThat(denied.getContentAsString()).contains("serverVersion");
  }

  @Test
  @DisplayName("ban 시 403을 반환한다")
  void doFilter_whenBanned_returns403() throws Exception {
    service.banIp("203.0.113.3", Duration.ofMinutes(5), "test");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols");
    request.addHeader("X-Forwarded-For", "203.0.113.3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean continued = new AtomicBoolean(false);

    filter.doFilter(request, response, trackingChain(continued));

    assertThat(continued).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("접근이 일시적으로 제한");
  }

  @Test
  @DisplayName("context-path를 제거한 뒤 path rule을 적용한다")
  void doFilter_whenAdminContextPath_matchesPathRule() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/api/auth/login");
    request.setContextPath("/admin/api");
    request.addHeader("X-Forwarded-For", "203.0.113.4");
    AtomicBoolean continued = new AtomicBoolean(false);
    FilterChain chain = trackingChain(continued);

    // auth rule limit=2
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    MockHttpServletResponse denied = new MockHttpServletResponse();
    filter.doFilter(request, denied, chain);

    assertThat(denied.getStatus()).isEqualTo(429);
  }

  @Test
  @DisplayName("resolvePathWithinApplication은 context-path를 제거한다")
  void resolvePathWithinApplication_stripsContextPath() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/auth/login");
    request.setContextPath("/admin/api");

    assertThat(AccessControlFilter.resolvePathWithinApplication(request)).isEqualTo("/auth/login");
  }

  private static FilterChain trackingChain(AtomicBoolean continued) {
    return new FilterChain() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response)
          throws IOException, ServletException {
        continued.set(true);
      }
    };
  }

  /** 테스트용 path rule 헬퍼 */
  private static final class PathRateLimitRuleSupport {
    private PathRateLimitRuleSupport() {}

    static void authRule(AccessControlProperties properties) {
      AccessControlProperties.PathRateLimitRule authRule =
          new AccessControlProperties.PathRateLimitRule();
      authRule.setPathPrefix("/auth");
      authRule.setLimit(2);
      authRule.setWindowSeconds(60);
      properties.setPathRules(java.util.List.of(authRule));
    }
  }
}
