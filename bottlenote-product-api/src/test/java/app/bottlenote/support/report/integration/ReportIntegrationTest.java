package app.bottlenote.support.report.integration;

import static app.bottlenote.review.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.constant.ReviewActiveStatus.DISABLED;
import static app.bottlenote.support.report.constant.ReviewReportType.ADVERTISEMENT;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.exception.ReportExceptionCode.ALREADY_REPORTED_REVIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
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
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reports/review")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(reviewReportRequest))
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    ReviewReport saved = reviewReportRepository.findAll().getFirst();
    assertNotNull(saved);
    assertEquals(review.getId(), saved.getReviewId());

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    ReviewReportResponse reviewReportResponse =
        mapper.convertValue(response.getData(), ReviewReportResponse.class);

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

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reports/review")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(reviewReportRequest))
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    ReviewReport saved =
        reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
    assertNotNull(saved);

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    ReviewReportResponse reviewReportResponse =
        mapper.convertValue(response.getData(), ReviewReportResponse.class);

    assertTrue(reviewReportResponse.success());

    Error error = Error.of(ALREADY_REPORTED_REVIEW);

    mockMvc
        .perform(
            post("/api/v1/reports/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reviewReportRequest))
                .header("Authorization", "Bearer " + getToken())
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(
            jsonPath("$.errors[?(@.code == 'ALREADY_REPORTED_REVIEW')].status")
                .value(error.status().name()))
        .andExpect(
            jsonPath("$.errors[?(@.code == 'ALREADY_REPORTED_REVIEW')].message")
                .value(error.message()));
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

    IntStream.range(1, 5)
        .forEach(
            (int i) -> {
              ReviewReport reviewReport =
                  ReviewReport.builder()
                      .reviewId(reviewReportRequest.reportReviewId())
                      .ipAddress(String.valueOf(i))
                      .type(ADVERTISEMENT)
                      .userId((long) i)
                      .reportContent("이 리뷰는 광고 리뷰입니다.")
                      .build();
              reviewReportRepository.save(reviewReport);
            });
    Review beforeReview =
        reviewRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);

    MvcResult authResult =
        mockMvc
            .perform(
                post("/api/v1/oauth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            new OauthRequest("test@test.com", null, SocialType.KAKAO, null, null)))
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String contentAsString = authResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    JsonNode dataNode = mapper.convertValue(response.getData(), JsonNode.class);
    String accessToken = dataNode.get("accessToken").asText();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reports/review")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(reviewReportRequest))
                    .header("Authorization", "Bearer " + accessToken)
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    ReviewReport savedReport =
        reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
    assertNotNull(savedReport);

    String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse res = mapper.readValue(content, GlobalResponse.class);
    ReviewReportResponse reviewReportResponse =
        mapper.convertValue(res.getData(), ReviewReportResponse.class);

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
    User targetUser = userTestFactory.persistUser("target@test.com", "신고대상유저");

    UserReportRequest userReportRequest =
        new UserReportRequest(targetUser.getId(), UserReportType.FRAUD, "아주 나쁜놈이에요 신고합니다.");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reports/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(userReportRequest))
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    UserReportResponse userReportResponse =
        mapper.convertValue(response.getData(), UserReportResponse.class);

    assertEquals(userReportResponse.getMessage(), SUCCESS.getMessage());
  }
}
