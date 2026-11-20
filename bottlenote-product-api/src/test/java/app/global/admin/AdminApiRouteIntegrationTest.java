package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import app.global.admin.docs.AdminOpenApiDocsController;
import app.global.security.SecurityPolicyConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

@Tag("integration")
@DisplayName("[integration] admin API 흡수 라우트 계약")
class AdminApiRouteIntegrationTest extends AdminApiIntegrationTestSupport {

  private static final Set<String> ADMIN_DOCS_ROUTES =
      Set.of(
          "GET /admin/api/v1/openapi.admin.json produces=application/json",
          "GET /admin/api/v1/openapi.admin.json.yaml produces=application/vnd.oai.openapi");

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Autowired private SecurityPolicyRegistry securityPolicyRegistry;

  @Test
  @DisplayName("admin 컨트롤러 라우트를 셀 때 main admin 앱의 외부 URL 156개와 매핑 조건까지 같다")
  void admin_컨트롤러_라우트는_main_admin_앱과_같다() throws IOException {
    Set<String> adminRoutes = routesOf(handler -> AdminApi.INSTANCE.isAdminController(handler));

    assertThat(adminRoutes).hasSize(156).isEqualTo(baseline("baseline/admin-routes-main.txt"));
  }

  @Test
  @DisplayName("admin이 아닌 라우트를 셀 때 main product 앱의 라우트와 매핑 조건까지 같다")
  void product_라우트는_main_product_앱과_같다() throws IOException {
    Set<String> productRoutes =
        routesOf(
            handler ->
                !AdminApi.INSTANCE.isAdminController(handler)
                    && handler != AdminOpenApiDocsController.class);

    assertThat(productRoutes).isEqualTo(baseline("baseline/product-routes-main.txt"));
  }

  @Test
  @DisplayName("admin 문서 라우트를 셀 때 admin 앱의 springdoc 문서 경로 두 개와 같다")
  void admin_문서_라우트는_admin_앱과_같다() {
    assertThat(routesOf(handler -> handler == AdminOpenApiDocsController.class))
        .isEqualTo(ADMIN_DOCS_ROUTES);
  }

  @Test
  @DisplayName("라우트 prefix를 확인할 때 admin 컨트롤러만 /admin/api 아래에 있고 product 라우트는 /admin 아래에 없다")
  void admin_prefix는_admin_컨트롤러에만_붙는다() {
    List<String> violations = new ArrayList<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              boolean admin =
                  AdminApi.INSTANCE.isAdminController(handler.getBeanType())
                      || handler.getBeanType() == AdminOpenApiDocsController.class;
              for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (admin && !pattern.startsWith(AdminApi.PATH_PREFIX + "/")) {
                  violations.add("admin 라우트가 /admin/api 밖에 있다: " + pattern);
                }
                if (!admin && pattern.startsWith("/admin")) {
                  violations.add("product 라우트가 /admin 아래에 있다: " + pattern);
                }
              }
            });

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("보안 정책을 조회할 때 admin 라우트는 선언한 정책 그대로이고 미수집 경로의 공개 기본값으로 떨어지지 않는다")
  void admin_라우트_보안_정책은_fail_open되지_않는다() {
    List<String> violations = new ArrayList<>();
    int requiredAuth = 0;
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
        handlerMapping.getHandlerMethods().entrySet()) {
      HandlerMethod handler = entry.getValue();
      if (!AdminApi.INSTANCE.isAdminController(handler.getBeanType())) {
        continue;
      }
      AuthType declared =
          SecurityPolicyRouteCollector.resolveAuthType(
              handler, SecurityPolicyConfig.FALLBACK_AUTH_TYPE);
      for (String method : methodsOf(entry.getKey())) {
        for (String pattern : entry.getKey().getPathPatternsCondition().getPatternValues()) {
          AuthType resolved = securityPolicyRegistry.resolve(method, concretePath(pattern));
          if (resolved != declared) {
            violations.add(method + " " + pattern + " 선언=" + declared + " 판정=" + resolved);
          }
          if (resolved == AuthType.REQUIRED_AUTH) {
            requiredAuth++;
          }
        }
      }
    }

    assertThat(violations).isEmpty();
    // 공개 경로는 로그인, 토큰 재발급, 에이전트 로그인 세 개뿐이다
    assertThat(requiredAuth).isEqualTo(156 - 3);
  }

  @Test
  @DisplayName("요청별 handler 체인을 볼 때 OSIV 인터셉터는 admin 경로에만 붙는다")
  void OSIV_인터셉터는_admin_경로에만_붙는다() throws Exception {
    // admin 앱은 spring.jpa.open-in-view 기본값(true)으로 동작했고 product는 꺼져 있다. 통합 앱은 admin 경로에서만 같은 동작을
    // 유지한다.
    assertThat(webRequestInterceptorsOf("/admin/api/v1/users")).isEqualTo(1);
    assertThat(webRequestInterceptorsOf("/admin/api/v2/curation-specs")).isEqualTo(1);
    assertThat(webRequestInterceptorsOf("/api/v1/alcohols/categories")).isZero();
  }

  private long webRequestInterceptorsOf(String path) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    ServletRequestPathUtils.parseAndCache(request);
    HandlerExecutionChain chain = handlerMapping.getHandler(request);
    assertThat(chain).as(path).isNotNull();
    return chain.getInterceptorList().stream()
        .filter(WebRequestHandlerInterceptorAdapter.class::isInstance)
        .count();
  }

  private Set<String> routesOf(Predicate<Class<?>> handlerType) {
    Set<String> routes = new TreeSet<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (!handlerType.test(handler.getBeanType())) {
                return;
              }
              String conditions = conditionsOf(info);
              for (String method : methodsOf(info)) {
                for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                  routes.add(method + " " + pattern + conditions);
                }
              }
            });
    return routes;
  }

  // /actuator/mappings의 requestMappingConditions와 같은 항목을 baseline 형식으로 적는다
  private static String conditionsOf(RequestMappingInfo info) {
    StringBuilder conditions = new StringBuilder();
    appendCondition(
        conditions, "consumes", mediaTypes(info.getConsumesCondition().getExpressions()));
    appendCondition(
        conditions, "produces", mediaTypes(info.getProducesCondition().getExpressions()));
    appendCondition(conditions, "params", nameValues(info.getParamsCondition().getExpressions()));
    appendCondition(conditions, "headers", nameValues(info.getHeadersCondition().getExpressions()));
    return conditions.toString();
  }

  private static void appendCondition(StringBuilder target, String name, List<String> values) {
    if (!values.isEmpty()) {
      target.append(' ').append(name).append('=').append(String.join(",", values));
    }
  }

  private static List<String> mediaTypes(Set<MediaTypeExpression> expressions) {
    return expressions.stream()
        .map(e -> (e.isNegated() ? "!" : "") + e.getMediaType())
        .sorted()
        .toList();
  }

  private static List<String> nameValues(Set<NameValueExpression<String>> expressions) {
    return expressions.stream()
        .map(
            e ->
                e.getValue() == null
                    ? (e.isNegated() ? "!" : "") + e.getName()
                    : e.getName() + (e.isNegated() ? "!=" : "=") + e.getValue())
        .sorted()
        .toList();
  }

  private static List<String> methodsOf(RequestMappingInfo info) {
    Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
    if (methods.isEmpty()) {
      return List.of("*");
    }
    return methods.stream().map(RequestMethod::name).toList();
  }

  // 경로 변수에는 리터럴 세그먼트와 겹치지 않는 값을 넣어 실제 요청 경로로 만든다
  static String concretePath(String pattern) {
    return pattern.replaceAll("\\{[^/]+}", "1");
  }

  private static Set<String> baseline(String resource) throws IOException {
    try (InputStream in = new ClassPathResource(resource).getInputStream()) {
      Set<String> routes = new TreeSet<>();
      for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          routes.add(trimmed);
        }
      }
      return routes;
    }
  }
}
