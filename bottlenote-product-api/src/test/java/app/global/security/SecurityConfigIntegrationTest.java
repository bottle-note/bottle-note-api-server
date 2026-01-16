package app.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import app.bottlenote.IntegrationTestSupport;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

@Tag("integration")
@DisplayName("[integration] SecurityConfig 악성 경로 차단 테스트")
class SecurityConfigIntegrationTest extends IntegrationTestSupport {

  private ListAppender<ILoggingEvent> logAppender;
  private Logger dispatcherLogger;

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
}
