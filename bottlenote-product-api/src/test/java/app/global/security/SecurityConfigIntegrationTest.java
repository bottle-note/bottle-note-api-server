package app.global.security;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.observability.visitor.VisitorTelemetry;
import app.bottlenote.observability.visitor.VisitorTelemetryFilter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.DispatcherServlet;

@Tag("integration")
@DisplayName("[integration] SecurityConfig 악성 경로 차단 테스트")
class SecurityConfigIntegrationTest extends IntegrationTestSupport {

  private ListAppender<ILoggingEvent> logAppender;
  private Logger dispatcherLogger;
  @Autowired private SecurityPolicyRegistry securityPolicyRegistry;

  @BeforeEach
  void setUpLogCapture() {
    dispatcherLogger = (Logger) LoggerFactory.getLogger(DispatcherServlet.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    dispatcherLogger.addAppender(logAppender);
  }

  @AfterEach
  void tearDownLogCapture() {
    dispatcherLogger.detachAppender(logAppender);
    logAppender.stop();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/.env",
        "/.git/config",
        "/wp-admin/test",
        "/phpmyadmin/test",
        "/.well-known/acme-challenge/test-token",
        "/backup/dump",
        "/.DS_Store"
      })
  @DisplayName("악성 경로 요청 시 403 반환 및 NoResourceFoundException 로그 미발생")
  void 악성_경로_차단_및_로그_미발생(String path) {
    // when
    var result = mockMvcTester.get().uri(path).exchange();

    // then - 403 Forbidden 반환
    result.assertThat().hasStatus(FORBIDDEN);

    // then - NoResourceFoundException 로그가 발생하지 않아야 함
    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .noneMatch(msg -> msg != null && msg.contains("NoResourceFoundException"));
  }

  @Test
  @DisplayName("내부 GraphQL 엔드포인트는 외부 HTTP 접근을 차단한다")
  void 내부_GraphQL_엔드포인트_외부_접근_차단() {
    mockMvcTester.get().uri("/graphiql").exchange().assertThat().hasStatus(FORBIDDEN);
    mockMvcTester.post().uri("/graphql").exchange().assertThat().hasStatus(FORBIDDEN);
  }

  @Test
  @DisplayName("비회원 조회 API는 토큰이 없어도 기존처럼 허용한다")
  void 비회원_조회_API_무토큰_허용() {
    var result = mockMvcTester.get().uri("/api/v1/alcohols/search").exchange();

    result.assertThat().hasStatusOk();
  }

  @ParameterizedTest
  @CsvSource({
    "GET, /api/v1/alcohols/categories, PUBLIC",
    "GET, /api/v1/alcohols/1, OPTIONAL_AUTH",
    "GET, /api/v1/rating, OPTIONAL_AUTH",
    "GET, /api/v1/rating/1, REQUIRED_AUTH",
    "GET, /api/v1/my-page/1, OPTIONAL_AUTH",
    "GET, /api/v1/my-page/1/my-bottle/reviews, REQUIRED_AUTH",
    "GET, /api/v1/my-page/1/my-bottle/ratings, REQUIRED_AUTH",
    "GET, /api/v1/my-page/1/my-bottle/picks, REQUIRED_AUTH",
    "GET, /api/v1/business-support, REQUIRED_AUTH",
    "GET, /api/v1/business-support/1, REQUIRED_AUTH",
    "PUT, /api/v1/likes, REQUIRED_AUTH",
    "GET, /error, PUBLIC"
  })
  @DisplayName("product-api SecurityPolicyRegistry가 실제 controller 정책을 수집한다")
  void product_api_SecurityPolicyRegistry_정책_수집(String method, String path, String expected) {
    AuthType expectedPolicy = AuthType.valueOf(expected);

    assertThat(securityPolicyRegistry.resolve(method, path)).isEqualTo(expectedPolicy);
  }

  @Test
  @DisplayName("수집된 handler가 없는 정상 경로는 인증 필수 fallback을 적용하지 않는다")
  void 미수집_정상_경로는_인증_필수_fallback_미적용() {
    assertThat(securityPolicyRegistry.resolve("GET", "/api/v1/not-found")).isEqualTo(PUBLIC);
  }

  @Test
  @DisplayName("선택 인증 API도 유효하지 않은 토큰이면 인증 실패한다")
  void 선택_인증_API_유효하지_않은_토큰_인증_실패() {
    var result =
        mockMvcTester
            .get()
            .uri("/api/v1/alcohols/search")
            .header("Authorization", "Bearer invalid.token")
            .exchange();

    result.assertThat().hasStatus(UNAUTHORIZED);
  }

  @Test
  @DisplayName("정상 API 경로는 차단되지 않는다")
  void 정상_API_경로_허용() {
    // when
    var result =
        mockMvcTester
            .get()
            .uri("/api/v1/alcohols/search")
            .header("Authorization", "Bearer " + getToken())
            .exchange();

    // then - 200 OK 또는 정상 응답 (403이 아님)
    result.assertThat().hasStatusOk();
  }

  @Test
  @DisplayName("VisitorTelemetry는 인증 사용자와 정규화한 클라이언트 IP를 수집한다")
  void VisitorTelemetry_요청_문맥을_수집한다() throws Exception {
    List<VisitorTelemetry> telemetries = new ArrayList<>();
    VisitorTelemetryFilter filter = SecurityConfig.visitorTelemetryFilter(telemetries::add);

    try {
      SecurityContextHolder.getContext()
          .setAuthentication(new TestingAuthenticationToken("42", null));
      MockHttpServletRequest ipv4Request =
          new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
      ipv4Request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.1");
      ipv4Request.setRemoteAddr("192.0.2.1");
      filter.doFilter(
          ipv4Request,
          new MockHttpServletResponse(),
          (request, response) ->
              ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK));

      MockHttpServletRequest ipv6Request =
          new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
      ipv6Request.addHeader("X-Forwarded-For", "unknown, 2001:0db8:0:0:0:0:0:1");
      ipv6Request.setRemoteAddr("192.0.2.2");
      filter.doFilter(
          ipv6Request,
          new MockHttpServletResponse(),
          (request, response) ->
              ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK));

      SecurityContextHolder.clearContext();
      MockHttpServletRequest fallbackRequest =
          new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
      fallbackRequest.addHeader("X-Forwarded-For", "unknown, invalid");
      fallbackRequest.setRemoteAddr("198.51.100.7");
      filter.doFilter(
          fallbackRequest,
          new MockHttpServletResponse(),
          (request, response) ->
              ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK));

      MockHttpServletRequest invalidRequest =
          new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
      invalidRequest.addHeader("X-Forwarded-For", "unknown");
      invalidRequest.setRemoteAddr("invalid");
      filter.doFilter(
          invalidRequest,
          new MockHttpServletResponse(),
          (request, response) ->
              ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK));
    } finally {
      SecurityContextHolder.clearContext();
    }

    assertThat(telemetries).hasSize(4);
    assertThat(telemetries.get(0).userId()).isEqualTo(42L);
    assertThat(telemetries.get(0).ipAddress()).isEqualTo("203.0.113.10");
    assertThat(telemetries.get(1).userId()).isEqualTo(42L);
    assertThat(telemetries.get(1).ipAddress()).isEqualTo("2001:db8::1");
    assertThat(telemetries.get(2).userId()).isNull();
    assertThat(telemetries.get(2).ipAddress()).isEqualTo("198.51.100.7");
    assertThat(telemetries.get(3).userId()).isNull();
    assertThat(telemetries.get(3).ipAddress()).isNull();
  }

  @Test
  @DisplayName("허용된 Origin의 preflight 요청에 해당 Origin을 반환한다")
  void 허용된_Origin_preflight_허용() {
    var result = preflight("https://bottle-note.com");

    result.assertThat().hasStatusOk();
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo("https://bottle-note.com");
  }

  @Test
  @DisplayName("허용되지 않은 Origin의 preflight 요청은 거부한다")
  void 허용되지_않은_Origin_preflight_거부() {
    var result = preflight("https://evil.example");

    result.assertThat().hasStatus(FORBIDDEN);
    assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

  private org.springframework.test.web.servlet.assertj.MvcTestResult preflight(String origin) {
    return mockMvcTester
        .options()
        .uri("/api/v1/alcohols/search")
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
        .exchange();
  }

  @Test
  @DisplayName("좋아요 명령 API는 토큰이 없으면 SecurityFilterChain에서 인증 실패한다")
  void 좋아요_명령_API_무토큰_인증_실패() {
    var result =
        mockMvcTester
            .put()
            .uri("/api/v1/likes")
            .contentType(APPLICATION_JSON)
            .content("{\"reviewId\":1,\"status\":\"LIKE\"}")
            .exchange();

    result.assertThat().hasStatus(UNAUTHORIZED);
  }

  @Test
  @DisplayName("필수 인증 API는 유효하지 않은 토큰이면 인증 실패한다")
  void 필수_인증_API_유효하지_않은_토큰_인증_실패() {
    var result =
        mockMvcTester
            .put()
            .uri("/api/v1/likes")
            .header("Authorization", "Bearer invalid.token")
            .contentType(APPLICATION_JSON)
            .content("{\"reviewId\":1,\"status\":\"LIKE\"}")
            .exchange();

    result.assertThat().hasStatus(UNAUTHORIZED);
  }

  @Test
  @DisplayName("평점 등록 API는 토큰이 없으면 SecurityFilterChain에서 인증 실패한다")
  void 평점_등록_API_무토큰_인증_실패() {
    var result =
        mockMvcTester
            .post()
            .uri("/api/v1/rating/register")
            .contentType(APPLICATION_JSON)
            .content("{\"alcoholId\":1,\"rating\":4.0}")
            .exchange();

    result.assertThat().hasStatus(UNAUTHORIZED);
  }
}
