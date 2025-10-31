package app.bottlenote.support.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bottlenote.support.report.service.DailyDataReportService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@DisplayName("[integration] [service] DailyDataReportService - TestContainers 실제 데이터 통합 테스트")
@Testcontainers
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
class DailyDataReportIntegrationTest {

  private static final Network network = Network.newNetwork();

  @Container
  protected static MySQLContainer<?> MY_SQL_CONTAINER =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
          .withReuse(true)
          .withNetwork(network)
          .withDatabaseName("bottlenote")
          .withUsername("root")
          .withPassword("root");

  @Container
  protected static GenericContainer<?> REDIS_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
          .withExposedPorts(6379)
          .withNetworkAliases("redis")
          .withNetwork(network)
          .withStartupAttempts(5)
          .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
          .withReuse(true)
          .withStartupTimeout(Duration.ofSeconds(30));

  static {
    CompletableFuture<Void> mysqlFuture = CompletableFuture.runAsync(MY_SQL_CONTAINER::start);
    CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(REDIS_CONTAINER::start);
    CompletableFuture.allOf(mysqlFuture, redisFuture).join();
  }

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
  }

  @Autowired private DailyDataReportService dailyDataReportService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @SpyBean private RestTemplate webhookRestTemplate;

  private LocalDate testDate;

  @BeforeEach
  void setUp() {
    testDate = LocalDate.now();
  }

  @AfterEach
  void tearDown() {
    // 테스트 데이터 정리 (외래 키 순서 고려)
    jdbcTemplate.execute("DELETE FROM likes WHERE create_at >= CURDATE() - INTERVAL 1 DAY");
    jdbcTemplate.execute(
        "DELETE FROM review_replies WHERE create_at >= CURDATE() - INTERVAL 1 DAY");
    jdbcTemplate.execute("DELETE FROM reviews WHERE create_at >= CURDATE() - INTERVAL 1 DAY");
    jdbcTemplate.execute(
        "DELETE FROM users WHERE email LIKE '%test.com' AND create_at >= CURDATE() - INTERVAL 1 DAY");
  }

  @DisplayName("시나리오1: 실제 데이터로 일일 리포트를 생성하고 집계가 정확한지 검증")
  @Test
  @Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-alcohol.sql"})
  @Transactional
  void 실제_데이터를_사용하여_일일_리포트가_정확하게_집계된다() {
    // given - Mock 응답 설정
    doReturn(ResponseEntity.ok("Success"))
        .when(webhookRestTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    // given - 오늘과 어제 데이터를 구분하여 생성
    LocalDateTime today = testDate.atStartOfDay();
    LocalDateTime yesterday = today.minusDays(1);

    // 오늘 가입한 신규 유저 3명
    createUser(today, "newuser1@test.com");
    createUser(today, "newuser2@test.com");
    createUser(today, "newuser3@test.com");

    // 어제 가입한 유저 (집계 제외)
    createUser(yesterday, "olduser@test.com");

    // 오늘 작성된 리뷰 2개
    createReview(today, 1L, 1L);
    createReview(today, 1L, 2L);

    // 어제 작성된 리뷰 (집계 제외)
    createReview(yesterday, 1L, 4L);

    // 오늘 작성된 댓글 4개
    createReply(today, 1L, 1L);
    createReply(today, 2L, 1L);
    createReply(today, 3L, 2L);
    createReply(today, 4L, 2L);

    // 오늘 생성된 좋아요 5개
    createLikes(today, 1L, 1L);
    createLikes(today, 2L, 1L);
    createLikes(today, 3L, 1L);
    createLikes(today, 4L, 2L);
    createLikes(today, 1L, 2L);

    // when - 일일 리포트 수집 및 전송
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(testDate, webhookUrl);

    // then - 웹훅이 정확히 1번 호출됨
    verify(webhookRestTemplate, times(1))
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    // 전송된 메시지 내용 검증
    org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(webhookRestTemplate)
        .postForEntity(anyString(), entityCaptor.capture(), eq(String.class));

    String body = entityCaptor.getValue().getBody().toString();
    assertNotNull(body);

    // 오늘 데이터만 집계되었는지 검증
    assertThat(body).contains("👥 **신규 유저**: 3명");
    assertThat(body).contains("✍️ **신규 리뷰**: 2개");
    assertThat(body).contains("💬 **신규 댓글**: 4개");
    assertThat(body).contains("❤️ **신규 좋아요**: 5개");
  }

  @DisplayName("시나리오2: 데이터가 없는 날도 정상적으로 0건으로 리포트된다")
  @Test
  void 데이터가_없는_날은_모든_집계가_0으로_리포트된다() {
    // given - Mock 응답 설정
    doReturn(ResponseEntity.ok("Success"))
        .when(webhookRestTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    // given - 과거 날짜로 데이터가 전혀 없는 상황
    LocalDate emptyDate = LocalDate.now().minusDays(10);

    // when - 빈 날짜로 리포트 수집
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(emptyDate, webhookUrl);

    // then - 웹훅 호출 검증
    verify(webhookRestTemplate, times(1))
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(webhookRestTemplate)
        .postForEntity(anyString(), entityCaptor.capture(), eq(String.class));

    String body = entityCaptor.getValue().getBody().toString();
    assertNotNull(body);

    // 모든 집계가 0으로 표시됨
    assertThat(body).contains("👥 **신규 유저**: 0명");
    assertThat(body).contains("✍️ **신규 리뷰**: 0개");
    assertThat(body).contains("💬 **신규 댓글**: 0개");
    assertThat(body).contains("❤️ **신규 좋아요**: 0개");
  }

  @DisplayName("시나리오3: 시간 경계값 - 자정 직전과 직후 데이터 구분")
  @Test
  @Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-alcohol.sql"})
  @Transactional
  void 자정을_기준으로_데이터가_정확하게_구분된다() {
    // given - Mock 응답 설정
    doReturn(ResponseEntity.ok("Success"))
        .when(webhookRestTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    // given - 자정 기준으로 경계값 테스트
    LocalDateTime todayMidnight = testDate.atStartOfDay();
    LocalDateTime beforeMidnight = todayMidnight.minusSeconds(1); // 23:59:59 (어제)
    LocalDateTime afterMidnight = todayMidnight.plusSeconds(1); // 00:00:01 (오늘)

    // 자정 직전 데이터 (어제 데이터 - 집계 제외)
    createUser(beforeMidnight, "before@test.com");
    createReview(beforeMidnight, 1L, 100L);

    // 자정 직후 데이터 (오늘 데이터 - 집계 포함)
    createUser(afterMidnight, "after@test.com");
    createReview(afterMidnight, 1L, 101L);

    // when - 오늘 날짜로 리포트 수집
    dailyDataReportService.collectAndSendDailyReport(
        testDate, "https://discord.com/api/webhooks/test");

    // then - 자정 이후 데이터만 집계됨
    org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(webhookRestTemplate)
        .postForEntity(anyString(), entityCaptor.capture(), eq(String.class));

    String body = entityCaptor.getValue().getBody().toString();

    // 자정 직후(00:00:01) 데이터만 포함
    assertThat(body).contains("👥 **신규 유저**: 1명");
    assertThat(body).contains("✍️ **신규 리뷰**: 1개");
  }

  @DisplayName("시나리오4: 웹훅 URL이 없으면 데이터 수집만 하고 전송하지 않는다")
  @Test
  @Sql(scripts = {"/init-script/init-user.sql"})
  @Transactional
  void 웹훅URL이_없으면_전송하지_않고_정상_처리된다() {
    // given - 실제 데이터 생성
    LocalDateTime today = testDate.atStartOfDay();
    createUser(today, "test@test.com");

    // when - 웹훅 URL을 null로 전달
    assertDoesNotThrow(() -> dailyDataReportService.collectAndSendDailyReport(testDate, null));

    // then - 웹훅 전송이 호출되지 않음
    verify(webhookRestTemplate, times(0))
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
  }

  @DisplayName("시나리오5: 대량 데이터 집계 성능 테스트")
  @Test
  @Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-alcohol.sql"})
  @Transactional
  void 대량의_데이터도_정상적으로_집계된다() {
    // given - Mock 응답 설정
    doReturn(ResponseEntity.ok("Success"))
        .when(webhookRestTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

    // given - 대량 데이터 생성 (유저 10명, 리뷰 20개, 댓글 30개, 좋아요 40개)
    LocalDateTime today = testDate.atStartOfDay();

    // 신규 유저 10명
    for (int i = 1; i <= 10; i++) {
      createUser(today, "bulkuser" + i + "@test.com");
    }

    // 신규 리뷰 20개
    for (int i = 1; i <= 20; i++) {
      createReview(today, 1L, (long) (i % 10 + 1));
    }

    // 신규 댓글 30개
    for (int i = 1; i <= 30; i++) {
      createReply(today, (long) (i % 10 + 1), (long) (i % 20 + 1));
    }

    // 신규 좋아요 40개
    for (int i = 1; i <= 40; i++) {
      createLikes(today, (long) (i % 10 + 1), (long) (i % 20 + 1));
    }

    // when - 대량 데이터 집계
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(testDate, webhookUrl);

    // then - 정확한 집계 결과 확인
    org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(webhookRestTemplate)
        .postForEntity(anyString(), entityCaptor.capture(), eq(String.class));

    String body = entityCaptor.getValue().getBody().toString();

    assertThat(body).contains("👥 **신규 유저**: 10명");
    assertThat(body).contains("✍️ **신규 리뷰**: 20개");
    assertThat(body).contains("💬 **신규 댓글**: 30개");
    assertThat(body).contains("❤️ **신규 좋아요**: 40개");
  }

  // Helper methods - 직접 SQL 사용
  private void createUser(LocalDateTime createDate, String email) {
    jdbcTemplate.update(
        "INSERT INTO users (email, age, nick_name, image_url, status, social_type, create_at, last_modify_at) "
            + "VALUES (?, ?, ?, ?, 'ACTIVE', '[]', ?, ?)",
        email,
        25,
        "TestUser_" + email,
        "https://test.com/image.jpg",
        createDate,
        createDate);
  }

  private void createReview(LocalDateTime createDate, Long alcoholId, Long userId) {
    jdbcTemplate.update(
        "INSERT INTO reviews (user_id, alcohol_id, content, create_at, last_modify_at) "
            + "VALUES (?, ?, ?, ?, ?)",
        userId,
        alcoholId,
        "Test review content",
        createDate,
        createDate);
  }

  private void createReply(LocalDateTime createDate, Long userId, Long reviewId) {
    jdbcTemplate.update(
        "INSERT INTO review_replies (review_id, user_id, status, content, create_at, last_modify_at) "
            + "VALUES (?, ?, 'NORMAL', ?, ?, ?)",
        reviewId,
        userId,
        "Test reply content",
        createDate,
        createDate);
  }

  private void createLikes(LocalDateTime createDate, Long userId, Long reviewId) {
    jdbcTemplate.update(
        "INSERT INTO likes (review_id, user_id, user_nick_name, status, create_at, last_modify_at) "
            + "VALUES (?, ?, 'TestUser', 'LIKE', ?, ?)",
        reviewId,
        userId,
        createDate,
        createDate);
  }
}
