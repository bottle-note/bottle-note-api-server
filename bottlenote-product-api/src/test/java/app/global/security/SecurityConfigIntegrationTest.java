package app.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
