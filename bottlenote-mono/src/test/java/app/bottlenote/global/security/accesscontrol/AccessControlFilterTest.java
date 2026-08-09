package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
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
    properties.setKeyNamespace("product");
    PathRateLimitRuleSupport.authRule(properties);
    service = new AccessControlService(store, properties, AccessControlMetrics.noop());
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
  @DisplayName("rate limit 초과 시 429와 Remaining 0을 반환한다")
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
    assertThat(denied.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    assertThat(denied.getContentAsString()).contains("요청이 너무 많습니다");
  }

  @Test
  @DisplayName("ban 시 403과 Retry-After를 반환한다")
  void doFilter_whenBanned_returns403() throws Exception {
    service.banIp("203.0.113.3", Duration.ofMinutes(5), "test");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols");
    request.addHeader("X-Forwarded-For", "203.0.113.3");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean continued = new AtomicBoolean(false);

    filter.doFilter(request, response, trackingChain(continued));

    assertThat(continued).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader("Retry-After")).isNotBlank();
    assertThat(response.getContentAsString()).contains("접근이 일시적으로 제한");
  }

  @Test
  @DisplayName("context-path를 제거한 뒤 path rule을 적용한다")
  void doFilter_whenAdminContextPath_matchesPathRule() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/api/v1/auth/login");
    request.setContextPath("/admin/api");
    request.addHeader("X-Forwarded-For", "203.0.113.4");
    AtomicBoolean continued = new AtomicBoolean(false);
    FilterChain chain = trackingChain(continued);

    filter.doFilter(request, new MockHttpServletResponse(), chain);
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    MockHttpServletResponse denied = new MockHttpServletResponse();
    filter.doFilter(request, denied, chain);

    assertThat(denied.getStatus()).isEqualTo(429);
  }

  @Test
  @DisplayName("resolvePathWithinApplication은 context-path를 제거한다")
  void resolvePathWithinApplication_stripsContextPath() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/v1/auth/login");
    request.setContextPath("/admin/api");

    assertThat(AccessControlFilter.resolvePathWithinApplication(request))
        .isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("HTTP method를 Service 정책 결정에 전달한다")
  void doFilter_whenReviewMethodDiffers_usesSeparateLimits() throws Exception {
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    AccessControlProperties properties = new AccessControlProperties();
    properties.setDefaultRateLimit(new RateLimitRule(100, 60));
    properties.setKeyNamespace("product");

    AccessControlProperties.PathRateLimitRule reviewsGet =
        new AccessControlProperties.PathRateLimitRule();
    reviewsGet.setPathPrefix("/api/v1/reviews");
    reviewsGet.setMethods(java.util.List.of("GET"));
    reviewsGet.setLimit(5);
    reviewsGet.setWindowSeconds(60);

    AccessControlProperties.PathRateLimitRule reviewsWrite =
        new AccessControlProperties.PathRateLimitRule();
    reviewsWrite.setPathPrefix("/api/v1/reviews");
    reviewsWrite.setMethods(java.util.List.of("POST", "PATCH", "DELETE"));
    reviewsWrite.setLimit(1);
    reviewsWrite.setWindowSeconds(60);

    properties.setPathRules(java.util.List.of(reviewsGet, reviewsWrite));
    AccessControlService methodService =
        new AccessControlService(store, properties, AccessControlMetrics.noop());
    AccessControlFilter methodFilter =
        new AccessControlFilter(methodService, new ObjectMapper().findAndRegisterModules());

    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v1/reviews");
    post.addHeader("X-Forwarded-For", "203.0.113.50");
    MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/reviews/1");
    get.addHeader("X-Forwarded-For", "203.0.113.50");
    AtomicBoolean continued = new AtomicBoolean(false);
    FilterChain chain = trackingChain(continued);

    // 쓰기 1회 허용 후 초과
    MockHttpServletResponse allowedPost = new MockHttpServletResponse();
    methodFilter.doFilter(post, allowedPost, chain);
    assertThat(allowedPost.getStatus()).isEqualTo(200);
    assertThat(allowedPost.getHeader("X-RateLimit-Limit")).isEqualTo("1");

    MockHttpServletResponse deniedPost = new MockHttpServletResponse();
    methodFilter.doFilter(post, deniedPost, chain);
    assertThat(deniedPost.getStatus()).isEqualTo(429);

    // 같은 IP의 GET은 별도 버킷
    MockHttpServletResponse allowedGet = new MockHttpServletResponse();
    methodFilter.doFilter(get, allowedGet, chain);
    assertThat(allowedGet.getStatus()).isEqualTo(200);
    assertThat(allowedGet.getHeader("X-RateLimit-Limit")).isEqualTo("5");
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

  private static final class PathRateLimitRuleSupport {
    private PathRateLimitRuleSupport() {}

    static void authRule(AccessControlProperties properties) {
      AccessControlProperties.PathRateLimitRule authRule =
          new AccessControlProperties.PathRateLimitRule();
      authRule.setPathPrefix("/v1/auth");
      authRule.setLimit(2);
      authRule.setWindowSeconds(60);
      properties.setPathRules(java.util.List.of(authRule));
    }
  }
}
