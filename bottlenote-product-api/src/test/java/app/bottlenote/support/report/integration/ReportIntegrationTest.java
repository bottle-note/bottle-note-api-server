package app.bottlenote.support.report.integration;

import static app.bottlenote.review.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.constant.ReviewActiveStatus.DISABLED;
import static app.bottlenote.support.report.constant.ReviewReportType.ADVERTISEMENT;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.exception.ReportExceptionCode.ALREADY_REPORTED_REVIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.support.report.constant.UserReportType;
import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.domain.ReviewReportRepository;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] HelpController")
class ReportIntegrationTest extends IntegrationTestSupport {

  @Autowired private ReviewReportRepository reviewReportRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @DisplayName("리뷰를 신고할 수 있다.")
  @Test
  void test_1() throws Exception {
    // given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(user, alcohol);
    ReviewReportRequest reviewReportRequest =
        new ReviewReportRequest(review.getId(), ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v1/reports/review")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(reviewReportRequest))
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    ReviewReport saved = reviewReportRepository.findAll().getFirst();
    assertNotNull(saved);
    assertEquals(review.getId(), saved.getReviewId());

    ReviewReportResponse reviewReportResponse = extractData(result, ReviewReportResponse.class);
    assertTrue(reviewReportResponse.success());
  }

  @DisplayName("유저는 하나의 리뷰에 대해 한번만 신고할 수 있다.")
  @Test
  void test_2() throws Exception {
    // given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(user, alcohol);
    ReviewReportRequest reviewReportRequest =
        new ReviewReportRequest(review.getId(), ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

    // when - 첫 번째 신고 (성공)
    MvcTestResult firstResult =
        mockMvcTester
            .post()
            .uri("/api/v1/reports/review")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(reviewReportRequest))
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    ReviewReport saved =
        reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
    assertNotNull(saved);

    ReviewReportResponse reviewReportResponse =
        extractData(firstResult, ReviewReportResponse.class);
    assertTrue(reviewReportResponse.success());

    // when - 두 번째 신고 (에러)
    Error error = Error.of(ALREADY_REPORTED_REVIEW);

    MvcTestResult secondResult =
        mockMvcTester
            .post()
            .uri("/api/v1/reports/review")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(reviewReportRequest))
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    secondResult
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors[0].status")
        .isEqualTo(error.status().name());
  }

  @DisplayName("서로 다른 IP로 5개의 신고가 누적되면 리뷰가 비활성화 된다.")
  @Test
  void test_3() throws Exception {
    // given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(user, alcohol);
    ReviewReportRequest reviewReportRequest =
        new ReviewReportRequest(review.getId(), ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

    for (int i = 1; i <= 4; i++) {
      User reporter = userTestFactory.persistUser("reporter-" + i, "신고자" + i);
      ReviewReport reviewReport =
          ReviewReport.builder()
              .reviewId(reviewReportRequest.reportReviewId())
              .ipAddress(String.valueOf(i))
              .type(ADVERTISEMENT)
              .userId(reporter.getId())
              .reportContent("이 리뷰는 광고 리뷰입니다.")
              .build();
      reviewReportRepository.save(reviewReport);
    }

    Review beforeReview =
        reviewRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);

    User fifthReporter = userTestFactory.persistUser("report-5th", "다섯번째신고자");

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v1/reports/review")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(reviewReportRequest))
            .header("Authorization", "Bearer " + getToken(fifthReporter).accessToken())
            .with(csrf())
            .exchange();

    // then
    ReviewReport savedReport =
        reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
    assertNotNull(savedReport);

    ReviewReportResponse reviewReportResponse = extractData(result, ReviewReportResponse.class);

    Review afterReview =
        reviewRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);

    assertNotNull(beforeReview);
    assertEquals(ACTIVE, beforeReview.getActiveStatus());
    log.info("누적 신고 5개 전 리뷰 상태: {}", beforeReview.getActiveStatus());
    assertTrue(reviewReportResponse.success());

    assertNotNull(afterReview);
    assertEquals(DISABLED, afterReview.getActiveStatus());
    log.info("누적 신고 5개 이후 리뷰 상태: {}", afterReview.getActiveStatus());
  }

  @DisplayName("유저를 신고할 수 있다.")
  @Test
  void test_4() throws Exception {
    // given
    User reporter = userTestFactory.persistUser("report-reporter", "신고자");
    User targetUser = userTestFactory.persistUser("report-target", "신고대상유저");

    UserReportRequest userReportRequest =
        new UserReportRequest(targetUser.getId(), UserReportType.FRAUD, "아주 나쁜놈이에요 신고합니다.");

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v1/reports/user")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(userReportRequest))
            .header("Authorization", "Bearer " + getToken(reporter).accessToken())
            .with(csrf())
            .exchange();

    // then
    UserReportResponse userReportResponse = extractData(result, UserReportResponse.class);
    assertEquals(userReportResponse.getMessage(), SUCCESS.getMessage());
  }
}
