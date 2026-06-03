package app.bottlenote.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
@DisplayName("[unit] product-api JWT 필터 접근 정책")
class JwtAuthenticationFilterPolicyTest {

  private final TestJwtAuthenticationFilter filter = new TestJwtAuthenticationFilter();

  @ParameterizedTest
  @CsvSource({
    "POST, /api/v1/oauth/login",
    "GET, /api/v1/regions",
    "GET, /api/v1/alcohols/categories"
  })
  @DisplayName("public 정책 경로는 JWT 필터를 건너뛴다")
  void shouldNotFilter_whenPublicEndpoint_returnsTrue(String method, String path) {
    assertThat(filter.shouldNotFilter(request(method, path))).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
    "GET, /api/v1/alcohols/search",
    "GET, /api/v1/reviews/1",
    "GET, /api/v1/regions/private",
    "POST, /api/v1/likes",
    "PUT, /api/v1/picks"
  })
  @DisplayName("optional-auth와 required-auth 정책 경로는 JWT 필터를 실행한다")
  void shouldNotFilter_whenAuthContextCanBeNeeded_returnsFalse(String method, String path) {
    assertThat(filter.shouldNotFilter(request(method, path))).isFalse();
  }

  @Test
  @DisplayName("무토큰 required-auth 요청에는 anonymous 인증 컨텍스트를 주입하지 않는다")
  void shouldUseAnonymousAuthentication_whenRequiredAuthWithoutToken_returnsFalse() {
    assertThat(
            JwtAuthenticationFilter.shouldUseAnonymousAuthentication("POST", "/api/v1/likes", null))
        .isFalse();
    assertThat(
            JwtAuthenticationFilter.shouldUseAnonymousAuthentication(
                "POST", "/api/v1/rating/register", ""))
        .isFalse();
  }

  @Test
  @DisplayName("무토큰 optional-auth 요청에는 기존 anonymous 인증 컨텍스트를 유지한다")
  void shouldUseAnonymousAuthentication_whenOptionalAuthWithoutToken_returnsTrue() {
    assertThat(
            JwtAuthenticationFilter.shouldUseAnonymousAuthentication(
                "GET", "/api/v1/alcohols/search", null))
        .isTrue();
  }

  private static HttpServletRequest request(String method, String path) {
    return new MockHttpServletRequest(method, path);
  }

  private static class TestJwtAuthenticationFilter extends JwtAuthenticationFilter {
    TestJwtAuthenticationFilter() {
      super(null);
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
      return super.shouldNotFilter(request);
    }
  }
}
