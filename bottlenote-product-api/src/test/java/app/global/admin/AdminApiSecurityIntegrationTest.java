package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.fixture.AdminUserTestFactory;
import app.global.security.SecurityPolicyConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * handler mapping에 등록된 admin 라우트 전체를 토큰 종류별로 호출한다.
 *
 * <p>admin 앱은 인증 실패를 entry point(Http403ForbiddenEntryPoint)의 sendError(403)로 응답한다. 같은 체인 구성이 유지되면
 * MockMvc 응답에도 403과 sendError 메시지가 남으므로, 업무 로직이 낸 403(본문이 있는 응답)과 구분할 수 있다.
 */
@Tag("integration")
@DisplayName("[integration] admin API 흡수 인가 경계")
class AdminApiSecurityIntegrationTest extends AdminApiIntegrationTestSupport {

  // 호출하면 관리자 계정이 비활성화되어 이후 요청의 토큰이 무효가 되므로 마지막에 호출한다
  private static final String WITHDRAW = "DELETE /admin/api/v1/auth/withdraw";

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Autowired private AdminUserTestFactory adminUserTestFactory;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Value("${security.jwt.secret-key}")
  private String secretKey;

  @Test
  @DisplayName("토큰 없이 admin 라우트를 호출할 때 인증 필수 라우트는 admin 앱처럼 403으로 거부하고 공개 라우트는 연다")
  void 토큰_없는_요청은_인증_필수_라우트에서_거부된다() throws Exception {
    List<String> violations = new ArrayList<>();
    for (AdminRoute route : adminRoutes()) {
      MockHttpServletResponse response = call(route, null);
      checkRejection(route, response, route.auth() == AuthType.REQUIRED_AUTH, violations);
    }

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("일반 사용자 토큰으로 admin 라우트를 호출할 때 인증 필수 라우트는 403으로 거부하고 공개 라우트는 연다")
  void 일반_사용자_토큰은_admin_라우트에서_거부된다() throws Exception {
    String userToken = getToken();
    List<String> violations = new ArrayList<>();
    for (AdminRoute route : adminRoutes()) {
      MockHttpServletResponse response = call(route, userToken);
      checkRejection(route, response, route.auth() == AuthType.REQUIRED_AUTH, violations);
    }

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("만료된 admin 토큰으로 인증 필수 admin 라우트를 호출할 때 403으로 거부한다")
  void 만료된_admin_토큰은_거부된다() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    String expired = expiredAdminToken(admin);
    List<String> violations = new ArrayList<>();
    for (AdminRoute route : adminRoutes()) {
      if (route.auth() != AuthType.REQUIRED_AUTH) {
        continue;
      }
      checkRejection(route, call(route, expired), true, violations);
    }

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("admin 토큰으로 admin 라우트를 호출할 때 모든 라우트가 보안 체인을 통과한다")
  void admin_토큰은_모든_admin_라우트를_통과한다() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    String adminToken =
        jwtTokenProvider
            .generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId())
            .accessToken();
    List<String> violations = new ArrayList<>();
    for (AdminRoute route : adminRoutes()) {
      checkRejection(route, call(route, adminToken), false, violations);
    }

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("admin 토큰으로 product 인증 필수 API를 호출할 때 product 체인이 처리하고 admin 권한으로 열리지 않는다")
  void admin_토큰은_product_인증_필수_API를_admin으로_통과하지_못한다() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    String adminToken =
        jwtTokenProvider
            .generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId())
            .accessToken();

    // product의 JwtAuthenticationManager는 isAdmin을 보지 않고 users에서 이메일로 사용자를 찾는다.
    // admin 이메일이 users에 없으면 필터에서 예외가 나는 기존 동작을 유지한다(실서버에서는 500).
    Throwable thrown =
        catchThrowable(
            () ->
                mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/v1/my-page/1/my-bottle/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)));
    assertThat(NestedExceptionUtils.getMostSpecificCause(thrown))
        .isInstanceOf(UsernameNotFoundException.class);

    // 공개 API는 토큰과 무관하게 기존처럼 응답한다
    MvcResult publicResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/alcohols/categories")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andReturn();
    assertThat(publicResult.getResponse().getStatus()).isEqualTo(200);
  }

  private void checkRejection(
      AdminRoute route,
      MockHttpServletResponse response,
      boolean expectRejected,
      List<String> violations) {
    boolean rejected = isSecurityRejection(response);
    if (rejected != expectRejected) {
      violations.add(
          route.display()
              + " 기대="
              + (expectRejected ? "거부" : "통과")
              + " 실제 status="
              + response.getStatus()
              + " errorMessage="
              + response.getErrorMessage());
    }
  }

  // 보안 체인의 거부는 sendError(403)로 끝나 본문 없이 에러 메시지만 남는다
  private static boolean isSecurityRejection(MockHttpServletResponse response) {
    return response.getStatus() == 403 && response.getErrorMessage() != null;
  }

  private MockHttpServletResponse call(AdminRoute route, String token) throws Exception {
    MockHttpServletRequestBuilder request =
        MockMvcRequestBuilders.request(HttpMethod.valueOf(route.method()), route.path());
    if (token != null) {
      request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    if (!"GET".equals(route.method()) && !"DELETE".equals(route.method())) {
      request.contentType(MediaType.APPLICATION_JSON).content("{}");
    }
    return mockMvc.perform(request).andReturn().getResponse();
  }

  private List<AdminRoute> adminRoutes() {
    List<AdminRoute> routes = new ArrayList<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (!AdminApi.INSTANCE.isAdminController(handler.getBeanType())) {
                return;
              }
              AuthType auth =
                  SecurityPolicyRouteCollector.resolveAuthType(
                      handler, SecurityPolicyConfig.FALLBACK_AUTH_TYPE);
              info.getMethodsCondition()
                  .getMethods()
                  .forEach(
                      method ->
                          info.getPathPatternsCondition()
                              .getPatternValues()
                              .forEach(
                                  pattern ->
                                      routes.add(
                                          new AdminRoute(
                                              method.name(),
                                              AdminApiRouteIntegrationTest.concretePath(pattern),
                                              auth))));
            });
    routes.sort(
        Comparator.comparing((AdminRoute route) -> WITHDRAW.equals(route.display()))
            .thenComparing(AdminRoute::display));
    assertThat(routes).hasSize(156);
    return routes;
  }

  private String expiredAdminToken(AdminUser admin) {
    String roles = String.join(",", admin.getRoles().stream().map(Enum::name).toList());
    return Jwts.builder()
        .setSubject(admin.getEmail())
        .claim("roles", roles)
        .claim("userId", admin.getId())
        .claim("isAdmin", true)
        .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
        .setExpiration(new Date(System.currentTimeMillis() - 60_000))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)), SignatureAlgorithm.HS512)
        .compact();
  }

  private record AdminRoute(String method, String path, AuthType auth) {
    String display() {
      return method + " " + path;
    }
  }
}
