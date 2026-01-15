package app.bottlenote.support.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.operation.utils.FakeWebhookRestTemplate;
import app.bottlenote.support.report.service.DailyDataReportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("integration")
@DisplayName("[integration] [service] DailyDataReportService")
class DailyDataReportIntegrationTest extends IntegrationTestSupport {
  @Autowired private DailyDataReportService dailyDataReportService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private FakeWebhookRestTemplate fakeWebhookRestTemplate;

  private LocalDate testDate;

  @BeforeEach
  void setUp() {
    testDate = LocalDate.now();
    fakeWebhookRestTemplate.clear();
  }

  @DisplayName("시나리오1: 실제 데이터로 일일 리포트를 생성하고 집계가 정확한지 검증")
  @Test
  void 실제_데이터를_사용하여_일일_리포트가_정확하게_집계된다() {
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
    assertThat(fakeWebhookRestTemplate.getCallCount()).isEqualTo(1);

    // 전송된 메시지 내용 검증
    String body = fakeWebhookRestTemplate.getLastRequestBody();
    assertNotNull(body);

    // 오늘 데이터만 집계되었는지 검증
    assertThat(body).contains("신규 유저**: 3명");
    assertThat(body).contains("신규 리뷰**: 2개");
    assertThat(body).contains("신규 댓글**: 4개");
    assertThat(body).contains("신규 좋아요**: 5개");
  }

  @DisplayName("시나리오2: 데이터가 없는 날은 웹훅을 전송하지 않는다")
  @Test
  void 데이터가_없는_날은_웹훅을_전송하지_않는다() {
    // given - 과거 날짜로 데이터가 전혀 없는 상황
    LocalDate emptyDate = LocalDate.now().minusDays(10);

    // when - 빈 날짜로 리포트 수집
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(emptyDate, webhookUrl);

    // then - 신규 데이터가 없으므로 웹훅이 호출되지 않음
    assertThat(fakeWebhookRestTemplate.wasNotCalled()).isTrue();
  }

  @DisplayName("시나리오3: 시간 경계값 - 자정 직전과 직후 데이터 구분")
  @Test
  void 자정을_기준으로_데이터가_정확하게_구분된다() {
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
    assertThat(fakeWebhookRestTemplate.wasCalled()).isTrue();

    String body = fakeWebhookRestTemplate.getLastRequestBody();

    // 자정 직후(00:00:01) 데이터만 포함
    assertThat(body).contains("신규 유저**: 1명");
    assertThat(body).contains("신규 리뷰**: 1개");
  }

  @DisplayName("시나리오4: 웹훅 URL이 없으면 데이터 수집만 하고 전송하지 않는다")
  @Test
  void 웹훅URL이_없으면_전송하지_않고_정상_처리된다() {
    // given - 실제 데이터 생성
    LocalDateTime today = testDate.atStartOfDay();
    createUser(today, "test@test.com");

    // when - 웹훅 URL을 null로 전달
    assertDoesNotThrow(() -> dailyDataReportService.collectAndSendDailyReport(testDate, null));

    // then - 웹훅 전송이 호출되지 않음
    assertThat(fakeWebhookRestTemplate.wasNotCalled()).isTrue();
  }

  @DisplayName("시나리오5: 대량 데이터 집계 성능 테스트")
  @Test
  void 대량의_데이터도_정상적으로_집계된다() {
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
    assertThat(fakeWebhookRestTemplate.wasCalled()).isTrue();

    String body = fakeWebhookRestTemplate.getLastRequestBody();

    assertThat(body).contains("신규 유저**: 10명");
    assertThat(body).contains("신규 리뷰**: 20개");
    assertThat(body).contains("신규 댓글**: 30개");
    assertThat(body).contains("신규 좋아요**: 40개");
  }

  @DisplayName("시나리오6: 신고와 문의 데이터가 포함된 리포트 생성")
  @Test
  void 신고와_문의_데이터가_포함된_리포트가_생성된다() {
    // given - 신고 및 문의 데이터 생성
    createReviewReport(1L, 1L, "WAITING");
    createReviewReport(1L, 2L, "PENDING");
    createUserReport(1L, 2L, "WAITING");
    createBusinessSupport(1L, "WAITING");
    createBusinessSupport(2L, "PENDING");

    LocalDateTime today = testDate.atStartOfDay();
    createUser(today, "user1@test.com");

    // when - 리포트 수집
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(testDate, webhookUrl);

    // then - 웹훅 호출 검증
    assertThat(fakeWebhookRestTemplate.wasCalled()).isTrue();

    String body = fakeWebhookRestTemplate.getLastRequestBody();
    assertNotNull(body);

    // 신고 및 문의 데이터 검증 (리뷰 신고 2건 + 유저 신고 1건 = 3건)
    assertThat(body).contains("미처리 신고**: 3건");
    assertThat(body).contains("미처리 문의**: 2건");
    assertThat(body).contains("신규 유저**: 1명");
  }

  @DisplayName("시나리오7: 0건인 항목은 리포트에서 제외된다")
  @Test
  void 값이_0인_항목은_메시지에_포함되지_않는다() {
    // given - 신규 유저 1명만 생성 (다른 데이터는 0)
    LocalDateTime today = testDate.atStartOfDay();
    createUser(today, "onlyuser@test.com");

    // when - 리포트 수집
    String webhookUrl = "https://discord.com/api/webhooks/test";
    dailyDataReportService.collectAndSendDailyReport(testDate, webhookUrl);

    // then - 웹훅 호출 검증
    assertThat(fakeWebhookRestTemplate.wasCalled()).isTrue();

    String body = fakeWebhookRestTemplate.getLastRequestBody();
    assertNotNull(body);

    // 신규 유저만 포함되고 나머지는 제외
    assertThat(body).contains("신규 유저**: 1명");
    assertThat(body).doesNotContain("**신규 리뷰**");
    assertThat(body).doesNotContain("**신규 댓글**");
    assertThat(body).doesNotContain("**신규 좋아요**");
    assertThat(body).doesNotContain("**미처리 신고**");
    assertThat(body).doesNotContain("**미처리 문의**");
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

  private void createReviewReport(Long userId, Long reviewId, String status) {
    jdbcTemplate.update(
        "INSERT INTO review_reports (user_id, review_id, type, report_content, status, ip_address, create_at, last_modify_at) "
            + "VALUES (?, ?, 'SPAM', 'Test report', ?, '127.0.0.1', NOW(), NOW())",
        userId,
        reviewId,
        status);
  }

  private void createUserReport(Long userId, Long reportUserId, String status) {
    jdbcTemplate.update(
        "INSERT INTO user_reports (user_id, report_user_id, type, report_content, status, create_at, last_modify_at) "
            + "VALUES (?, ?, 'SPAM', 'Test report', ?, NOW(), NOW())",
        userId,
        reportUserId,
        status);
  }

  private void createBusinessSupport(Long userId, String status) {
    jdbcTemplate.update(
        "INSERT INTO business_supports (user_id, title, content, contact, status, create_at, last_modify_at) "
            + "VALUES (?, 'Test Business Support', 'Test content', 'test@test.com', ?, NOW(), NOW())",
        userId,
        status);
  }
}
