package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("unit")
class VisitorIdentityFilterTest {
  private static final String ID = "c74a0e2c-bf9d-4edf-b122-44d42f621d9a";

  @Test
  @DisplayName("유효한 기존 쿠키가 있을 때 공유 발급기를 호출하지 않는다")
  void 기존_쿠키를_우선한다() throws Exception {
    var request = request();
    request.setCookies(new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, ID));
    var response = new MockHttpServletResponse();
    var filter =
        new VisitorTelemetryFilter(
            event -> {},
            () -> null,
            req -> "203.0.113.1",
            (ip, ua) -> {
              throw new AssertionError("resolver must not be called");
            });
    filter.doFilter(request, response, (req, res) -> {});
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ssr", "SSR", " ssr "})
  @DisplayName("SSR 요청일 때 발급기와 쿠키와 수집을 모두 건너뛴다")
  void SSR은_발급하지_않는다(String caller) throws Exception {
    var request = request();
    request.addHeader("X-Bottlenote-Caller", caller);
    var response = new MockHttpServletResponse();
    var filter =
        new VisitorTelemetryFilter(
            event -> {
              throw new AssertionError("no event");
            },
            () -> null,
            req -> "203.0.113.1",
            (ip, ua) -> {
              throw new AssertionError("no identity");
            });
    filter.doFilter(request, response, (req, res) -> {});
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(request.getAttribute(VisitorTelemetryFilter.VISITOR_ID_ATTRIBUTE)).isNull();
  }

  @Test
  @DisplayName("잘못된 쿠키일 때 공유 번호를 핸들러와 쿠키와 수집에 함께 사용한다")
  void 잘못된_쿠키를_공유_번호로_교체한다() throws Exception {
    var request = request();
    request.setCookies(new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, "broken"));
    var response = new MockHttpServletResponse();
    List<VisitorTelemetry> events = new ArrayList<>();
    var filter =
        new VisitorTelemetryFilter(
            events::add,
            () -> 42L,
            req -> "203.0.113.1",
            (ip, ua) -> {
              assertThat(ip).isEqualTo("203.0.113.1");
              assertThat(ua).isEqualTo("Browser/100");
              return ID;
            });
    filter.doFilter(
        request,
        response,
        (req, res) ->
            assertThat(req.getAttribute(VisitorTelemetryFilter.VISITOR_ID_ATTRIBUTE)).isNotNull());
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
        .startsWith(VisitorTelemetryFilter.VISITOR_COOKIE_NAME + "=" + ID + ";");
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().visitorId())
        .isEqualTo(request.getAttribute(VisitorTelemetryFilter.VISITOR_ID_ATTRIBUTE));
    assertThat(events.getFirst().userId()).isEqualTo(42L);
  }

  @Test
  @DisplayName("동일한 방문자 쿠키를 사용해도 로그인 계정은 각각 기록한다")
  void 회원_계정을_구분한다() throws Exception {
    List<VisitorTelemetry> events = new ArrayList<>();
    for (long user : new long[] {1L, 2L}) {
      var request = request();
      request.setCookies(new Cookie(VisitorTelemetryFilter.VISITOR_COOKIE_NAME, ID));
      new VisitorTelemetryFilter(
              events::add,
              () -> user,
              req -> "203.0.113.1",
              (ip, ua) -> {
                throw new AssertionError("no resolver");
              })
          .doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }
    assertThat(events)
        .extracting(VisitorTelemetry::visitorId)
        .containsOnly(events.getFirst().visitorId());
    assertThat(events).extracting(VisitorTelemetry::userId).containsExactly(1L, 2L);
  }

  private static MockHttpServletRequest request() {
    var request = new MockHttpServletRequest("GET", "/api/v1/alcohols/5004");
    request.addHeader(HttpHeaders.USER_AGENT, "Browser/100");
    return request;
  }
}
