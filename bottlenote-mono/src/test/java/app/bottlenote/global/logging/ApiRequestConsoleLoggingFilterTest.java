package app.bottlenote.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("unit")
@DisplayName("[unit] API 요청 콘솔 로깅 필터")
class ApiRequestConsoleLoggingFilterTest {

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
  }

  @Test
  @DisplayName("요청 처리 후 메서드, 경로, 상태, 처리 시간을 출력한다")
  void 요청_처리_후_요청_메타데이터를_출력한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alcohols/search");
    request.setQueryString("keyword=macallan&access_token=abc123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        (servletRequest, servletResponse) ->
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_CREATED);

    filter.doFilter(request, response, filterChain);

    assertThat(logAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .anySatisfy(
            message -> {
              assertThat(message).contains("method=GET");
              assertThat(message).contains("path=/api/v1/alcohols/search");
              assertThat(message).contains("queryPresent=true");
              assertThat(message).doesNotContain("keyword=macallan");
              assertThat(message).doesNotContain("abc123");
              assertThat(message).contains("status=201");
              assertThat(message).containsPattern("durationMs=\\d+");
            });
  }
}
