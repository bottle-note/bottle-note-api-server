package app.bottlenote.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("unit")
@DisplayName("[unit] API 요청 콘솔 로깅 필터")
class ApiRequestConsoleLoggingFilterTest {

  private static final String VISITOR_ID = "c74a0e2c-bf9d-4edf-b122-44d42f621d9a";

  private final ApiRequestConsoleLoggingFilter filter = new ApiRequestConsoleLoggingFilter();
  private final Logger logger =
      (Logger) LoggerFactory.getLogger(ApiRequestConsoleLoggingFilter.class);
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUpLogCapture() {
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDownLogCapture() {
    logger.detachAppender(logAppender);
    logAppender.stop();
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  @DisplayName("기존 방문자의 성공 요청은 방문자와 회원 식별 정보를 출력한다")
  void 기존_방문자의_성공_요청을_출력한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    request.setQueryString("keyword=macallan&access_token=abc123");
    request.setCookies(new Cookie(ApiRequestConsoleLoggingFilter.VISITOR_COOKIE_NAME, VISITOR_ID));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        (servletRequest, servletResponse) ->
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_CREATED);
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("42", "password"));
    MDC.put("traceId", "trace-123");

    filter.doFilter(request, response, filterChain);

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .anySatisfy(
            message -> {
              assertThat(message).contains("traceId=trace-123");
              assertThat(message).containsPattern("visitorId=[0-9a-f]{64}");
              assertThat(message).doesNotContain(VISITOR_ID);
              assertThat(message).contains("userId=42");
              assertThat(message).contains("method=GET");
              assertThat(message).contains("path=/api/v1/alcohols/search");
              assertThat(message).contains("queryPresent=true");
              assertThat(message).doesNotContain("keyword=macallan");
              assertThat(message).doesNotContain("abc123");
              assertThat(message).contains("status=201");
              assertThat(message).containsPattern("durationMs=\\d+");
            });
  }

  @Test
  @DisplayName("방문자 쿠키가 없는 성공 요청은 활동 로그 없이 쿠키를 발급한다")
  void 방문자_쿠키가_없는_성공_요청은_쿠키를_발급한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, successfulChain());

    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .startsWith(ApiRequestConsoleLoggingFilter.VISITOR_COOKIE_NAME + "=")
        .contains("Path=/")
        .contains("Max-Age=31536000")
        .contains("Secure")
        .contains("HttpOnly")
        .contains("SameSite=Lax")
        .doesNotContain("Domain=");
    assertThat(extractCookieValue(setCookie)).satisfies(value -> UUID.fromString(value));
    assertThat(activityMessages()).isEmpty();
  }

  @Test
  @DisplayName("형식이 잘못된 방문자 쿠키는 기록하지 않고 교체한다")
  void 잘못된_방문자_쿠키는_교체한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    request.setCookies(
        new Cookie(ApiRequestConsoleLoggingFilter.VISITOR_COOKIE_NAME, "client-created-value"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, successfulChain());

    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).startsWith(ApiRequestConsoleLoggingFilter.VISITOR_COOKIE_NAME + "=");
    assertThat(extractCookieValue(setCookie)).isNotEqualTo("client-created-value");
    assertThat(activityMessages()).isEmpty();
  }

  @ParameterizedTest(name = "status={0}")
  @ValueSource(ints = {302, 400, 500})
  @DisplayName("2xx가 아닌 응답은 쿠키와 활동 로그를 남기지 않는다")
  void 성공하지_않은_응답은_기록하지_않는다(int status) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    request.setCookies(new Cookie(ApiRequestConsoleLoggingFilter.VISITOR_COOKIE_NAME, VISITOR_ID));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (servletRequest, servletResponse) ->
            ((HttpServletResponse) servletResponse).setStatus(status);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(activityMessages()).isEmpty();
  }

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
    "OPTIONS, /api/v1/alcohols/search",
    "HEAD, /api/v1/alcohols/search",
    "GET, /actuator/health",
    "GET, /favicon.ico"
  })
  @DisplayName("활동 대상이 아닌 요청은 필터링하지 않는다")
  void 활동_대상이_아닌_요청은_기록하지_않는다(String method, String path) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, successfulChain());

    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(activityMessages()).isEmpty();
  }

  private FilterChain successfulChain() {
    return (servletRequest, servletResponse) ->
        ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);
  }

  private String extractCookieValue(String setCookie) {
    return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
  }

  private List<String> activityMessages() {
    return logAppender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .filter(message -> message.startsWith("api_request"))
        .toList();
  }
}
