package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

@Tag("unit")
@DisplayName("[unit] VisitorTelemetry 필터")
class VisitorTelemetryFilterTest {

  private static final String VISITOR_ID = "c74a0e2c-bf9d-4edf-b122-44d42f621d9a";
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-16T15:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  @DisplayName("최초 성공 요청은 쿠키를 발급하고 같은 방문자를 즉시 발행한다")
  void 최초_성공_요청을_발행한다() throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(
            publisher, () -> 42L, request -> "203.0.113.10", FIXED_CLOCK);
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, successfulChain());

    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    String issuedVisitorId = extractCookieValue(setCookie);
    assertThat(setCookie)
        .startsWith(VisitorTelemetryFilter.VISITOR_COOKIE_NAME + "=")
        .contains("Path=/")
        .contains("Max-Age=31536000")
        .contains("Secure")
        .contains("HttpOnly")
        .contains("SameSite=Lax")
        .doesNotContain("Domain=");
    assertThat(issuedVisitorId).satisfies(value -> UUID.fromString(value));
    assertThat(publisher.single()).satisfies(telemetry -> {
      assertThat(telemetry.visitorId()).isEqualTo(sha256(issuedVisitorId));
      assertThat(telemetry.userId()).isEqualTo(42L);
      assertThat(telemetry.ipAddress()).isEqualTo("203.0.113.10");
      assertThat(telemetry.occurredAt()).hasToString("2026-07-17T00:00");
      assertThat(telemetry.statusCode()).isEqualTo(200);
    });
  }

  @Test
  @DisplayName("기존 쿠키와 라우트 패턴을 재사용하고 민감 쿼리 값만 치환한다")
  void 기존_쿠키와_정규화_경로를_발행한다() throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletRequest request = request("GET", "/api/v1/alcohols/123");
    request.setCookies(new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, VISITOR_ID));
    request.setQueryString(
        "keyword=macallan&access_token=abc123&EMAIL=user%40example.com&page=2&code"
            + "&client_secret=value&phone_number=01012345678&apiKey=key");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/alcohols/{alcoholId}");
    MDC.put("traceId", "trace-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      filter.doFilter(request, response, successfulChain());
    } finally {
      MDC.clear();
    }

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(publisher.single()).satisfies(telemetry -> {
      assertThat(telemetry.visitorId()).isEqualTo(sha256(VISITOR_ID));
      assertThat(telemetry.userId()).isNull();
      assertThat(telemetry.ipAddress()).isNull();
      assertThat(telemetry.traceId()).isEqualTo("trace-123");
      assertThat(telemetry.requestPath())
          .isEqualTo(
              "/api/v1/alcohols/123?keyword=macallan&access_token=[REDACTED]"
                  + "&EMAIL=[REDACTED]&page=2&code=[REDACTED]"
                  + "&client_secret=[REDACTED]&phone_number=[REDACTED]&apiKey=[REDACTED]");
      assertThat(telemetry.requestUri()).isEqualTo("/api/v1/alcohols/123");
      assertThat(telemetry.normalizedRequestPath())
          .isEqualTo("/api/v1/alcohols/{alcoholId}");
    });
  }

  @Test
  @DisplayName("잘못된 쿠키는 교체하고 교체한 방문자를 즉시 발행한다")
  void 잘못된_쿠키를_교체하고_발행한다() throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletRequest request = request("GET", "/api/v1/alcohols/search");
    request.setCookies(
        new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, "client-created-value"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, successfulChain());

    String issuedVisitorId = extractCookieValue(response.getHeader(HttpHeaders.SET_COOKIE));
    assertThat(issuedVisitorId).isNotEqualTo("client-created-value");
    assertThat(publisher.single().visitorId()).isEqualTo(sha256(issuedVisitorId));
  }

  @ParameterizedTest(name = "status={0}")
  @ValueSource(ints = {302, 400, 500})
  @DisplayName("2xx가 아닌 응답은 쿠키와 텔레메트리를 남기지 않는다")
  void 성공하지_않은_응답은_기록하지_않는다(int status) throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletRequest request = request("GET", "/api/v1/alcohols/search");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (servletRequest, servletResponse) ->
            ((HttpServletResponse) servletResponse).setStatus(status);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(publisher.telemetries).isEmpty();
  }

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
    "OPTIONS, /api/v1/alcohols/search",
    "HEAD, /api/v1/alcohols/search",
    "GET, /actuator/health",
    "GET, /favicon.ico"
  })
  @DisplayName("수집 대상이 아닌 요청은 기록하지 않는다")
  void 수집_대상이_아닌_요청은_기록하지_않는다(String method, String path) throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request(method, path), response, successfulChain());

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(publisher.telemetries).isEmpty();
  }

  @Test
  @DisplayName("Android와 iOS WebView 요청을 정규화한다")
  void WebView_요청을_정규화한다() throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletRequest androidRequest = request("GET", "/api/v1/alcohols/search");
    androidRequest.addHeader(
        HttpHeaders.USER_AGENT,
        "Mozilla/5.0 (Linux; Android 10; K; wv) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Version/4.0 Chrome/143.0.0.0 Mobile Safari/537.36");
    MockHttpServletRequest iosRequest = request("GET", "/api/v1/alcohols/search");
    iosRequest.addHeader(
        HttpHeaders.USER_AGENT,
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 Mobile/15E148");

    filter.doFilter(androidRequest, new MockHttpServletResponse(), successfulChain());
    filter.doFilter(iosRequest, new MockHttpServletResponse(), successfulChain());

    assertThat(publisher.telemetries.get(0)).satisfies(telemetry -> {
      assertThat(telemetry.deviceType()).isEqualTo("모바일");
      assertThat(telemetry.operatingSystem()).isEqualTo("Android");
      assertThat(telemetry.browser()).isEqualTo("Chrome");
      assertThat(telemetry.browserMajorVersion()).isEqualTo("143");
      assertThat(telemetry.webview()).isTrue();
    });
    assertThat(publisher.telemetries.get(1)).satisfies(telemetry -> {
      assertThat(telemetry.deviceType()).isEqualTo("모바일");
      assertThat(telemetry.operatingSystem()).isEqualTo("iOS");
      assertThat(telemetry.browser()).isEqualTo("WebView");
      assertThat(telemetry.webview()).isTrue();
    });
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("browserUserAgents")
  @DisplayName("주요 브라우저와 주 버전을 정규화한다")
  void 주요_브라우저를_정규화한다(
      String expectedBrowser, String expectedVersion, String userAgent) throws Exception {
    CapturingPublisher publisher = new CapturingPublisher();
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(publisher, () -> null, request -> null, FIXED_CLOCK);
    MockHttpServletRequest request = request("GET", "/api/v1/alcohols/search");
    request.addHeader(HttpHeaders.USER_AGENT, userAgent);

    filter.doFilter(request, new MockHttpServletResponse(), successfulChain());

    assertThat(publisher.single()).satisfies(telemetry -> {
      assertThat(telemetry.browser()).isEqualTo(expectedBrowser);
      assertThat(telemetry.browserMajorVersion()).isEqualTo(expectedVersion);
    });
  }

  @Test
  @DisplayName("발행 실패에도 응답 상태와 본문을 유지한다")
  void 발행_실패가_응답에_영향을_주지_않는다() throws Exception {
    VisitorTelemetryFilter filter =
        new VisitorTelemetryFilter(
            telemetry -> {
              throw new IllegalStateException("redis down");
            },
            () -> null,
            request -> null);
    MockHttpServletRequest request = request("GET", "/api/v1/alcohols/search");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (servletRequest, servletResponse) -> {
          ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_CREATED);
          servletResponse.getWriter().write("response-body");
        };

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEqualTo("response-body");
  }

  private MockHttpServletRequest request(String method, String path) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setCookies(new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, VISITOR_ID));
    return request;
  }

  private FilterChain successfulChain() {
    return (servletRequest, servletResponse) ->
        ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);
  }

  private String extractCookieValue(String setCookie) {
    return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  private static List<Object[]> browserUserAgents() {
    return List.of(
        new Object[] {
          "Whale",
          "4",
          "Mozilla/5.0 (Macintosh) AppleWebKit/537.36 Chrome/132.0 Safari/537.36 Whale/4.30"
        },
        new Object[] {
          "Edge",
          "131",
          "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/131.0 Safari/537.36 Edg/131.0"
        },
        new Object[] {
          "SamsungInternet",
          "27",
          "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/125.0 "
              + "Mobile Safari/537.36 SamsungBrowser/27.0"
        },
        new Object[] {
          "Chrome",
          "143",
          "Mozilla/5.0 (Macintosh) AppleWebKit/537.36 Chrome/143.0 Safari/537.36"
        },
        new Object[] {
          "Firefox", "135", "Mozilla/5.0 (Macintosh; Intel Mac OS X) Gecko/20100101 Firefox/135.0"
        },
        new Object[] {
          "Safari",
          "18",
          "Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15"
        });
  }

  private static final class CapturingPublisher implements VisitorTelemetryPublisher {

    private final List<VisitorTelemetry> telemetries = new ArrayList<>();

    @Override
    public void publish(VisitorTelemetry telemetry) {
      telemetries.add(telemetry);
    }

    private VisitorTelemetry single() {
      assertThat(telemetries).singleElement();
      return telemetries.getFirst();
    }
  }
}
