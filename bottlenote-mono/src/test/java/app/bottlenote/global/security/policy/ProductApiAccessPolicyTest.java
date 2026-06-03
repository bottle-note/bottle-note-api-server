package app.bottlenote.global.security.policy;

import static app.bottlenote.global.security.policy.ProductApiAccessPolicy.AccessType.OPTIONAL_AUTH;
import static app.bottlenote.global.security.policy.ProductApiAccessPolicy.AccessType.PUBLIC;
import static app.bottlenote.global.security.policy.ProductApiAccessPolicy.AccessType.REQUIRED_AUTH;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("[unit] product-api JWT 접근 정책")
class ProductApiAccessPolicyTest {

  @ParameterizedTest
  @CsvSource({
    "POST, /api/v1/oauth/login",
    "GET, /api/v1/regions",
    "GET, /api/v1/alcohols/categories",
    "GET, /api/v1/banners",
    "GET, /api/v2/auth/apple/nonce"
  })
  @DisplayName("인증 컨텍스트가 필요 없는 공개 API는 public 정책이다")
  void resolve_whenPublicEndpoint_returnsPublic(String method, String path) {
    assertThat(ProductApiAccessPolicy.resolve(method, path)).isEqualTo(PUBLIC);
  }

  @ParameterizedTest
  @CsvSource({
    "GET, /api/v1/alcohols/search",
    "GET, /api/v1/alcohols/1",
    "GET, /api/v1/popular",
    "GET, /api/v1/reviews/1",
    "GET, /api/v1/reviews/detail/10",
    "GET, /api/v1/reviews/explore/standard",
    "GET, /api/v1/review/reply/1",
    "GET, /api/v1/rating/1",
    "GET, /api/v1/my-page/1"
  })
  @DisplayName("비회원 접근과 로그인 사용자 개인화가 모두 필요한 조회 API는 optional-auth 정책이다")
  void resolve_whenOptionalAuthEndpoint_returnsOptionalAuth(String method, String path) {
    assertThat(ProductApiAccessPolicy.resolve(method, path)).isEqualTo(OPTIONAL_AUTH);
  }

  @ParameterizedTest
  @CsvSource({
    "PUT, /api/v1/picks",
    "POST, /api/v1/likes",
    "POST, /api/v1/rating/register",
    "PUT, /api/v1/rating/1",
    "POST, /api/v1/review/reply",
    "DELETE, /api/v1/review/reply/1/1",
    "POST, /api/v1/reviews",
    "PATCH, /api/v1/reviews/1",
    "DELETE, /api/v1/reviews/1",
    "GET, /api/v1/users/me",
    "GET, /api/v1/history",
    "POST, /api/v1/blocks"
  })
  @DisplayName("사용자 식별이 필수인 명령 API는 required-auth 정책이다")
  void resolve_whenRequiredAuthEndpoint_returnsRequiredAuth(String method, String path) {
    assertThat(ProductApiAccessPolicy.resolve(method, path)).isEqualTo(REQUIRED_AUTH);
  }

  @Test
  @DisplayName("required-auth predicate는 기준 경로와 하위 경로를 모두 인증 필수로 판단한다")
  void requiresAuthentication_whenPrefixRule_matchesBaseAndChildren() {
    assertThat(ProductApiAccessPolicy.requiresAuthentication("PUT", "/api/v1/likes")).isTrue();
    assertThat(ProductApiAccessPolicy.requiresAuthentication("PUT", "/api/v1/likes/1")).isTrue();
    assertThat(ProductApiAccessPolicy.requiresAuthentication("POST", "/api/v1/rating/register"))
        .isTrue();
  }
}
