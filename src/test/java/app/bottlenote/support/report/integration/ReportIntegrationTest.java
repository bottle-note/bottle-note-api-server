package app.bottlenote.support.report.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import static app.bottlenote.review.domain.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.domain.constant.ReviewActiveStatus.DISABLED;
import app.bottlenote.support.report.domain.ReviewReport;
import static app.bottlenote.support.report.domain.constant.ReviewReportType.ADVERTISEMENT;
import app.bottlenote.support.report.domain.constant.UserReportType;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.exception.ReportExceptionCode.ALREADY_REPORTED_REVIEW;
import app.bottlenote.support.report.repository.ReviewReportRepository;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] HelpController")
@WithMockUser
class ReportIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ReviewReportRepository reviewReportRepository;
	@Autowired
	private ReviewRepository reviewRepository;

	@DisplayName("리뷰를 신고할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_1() throws Exception {
		// given
		ReviewReportRequest reviewReportRequest = new ReviewReportRequest(1L, ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

		final Long reviewReportId = 1L;
		MvcResult result = mockMvc.perform(post("/api/v1/reports/review")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewReportRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		ReviewReport saved = reviewReportRepository.findById(reviewReportId).orElse(null);
		assertNotNull(saved);

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		ReviewReportResponse reviewReportResponse = mapper.convertValue(response.getData(), ReviewReportResponse.class);

		assertTrue(reviewReportResponse.success());
	}

	@DisplayName("유저는 하나의 리뷰에 대해 한번만 신고할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_2() throws Exception {
		// given
		ReviewReportRequest reviewReportRequest = new ReviewReportRequest(1L, ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

		MvcResult result = mockMvc.perform(post("/api/v1/reports/review")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewReportRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		ReviewReport saved = reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
		assertNotNull(saved);

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		ReviewReportResponse reviewReportResponse = mapper.convertValue(response.getData(), ReviewReportResponse.class);

		assertTrue(reviewReportResponse.success());

		Error error = Error.of(ALREADY_REPORTED_REVIEW);

		mockMvc.perform(post("/api/v1/reports/review")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewReportRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.errors[?(@.code == 'ALREADY_REPORTED_REVIEW')].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == 'ALREADY_REPORTED_REVIEW')].message").value(error.message()));
	}

	@DisplayName("서로 다른 IP로 5개의 신고가 누적되면 리뷰가 비활성화 된다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_3() throws Exception {
		// given
		ReviewReportRequest reviewReportRequest = new ReviewReportRequest(1L, ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");

		IntStream.range(1, 5).forEach((int i) -> {
			ReviewReport reviewReport = ReviewReport.builder()
				.reviewId(reviewReportRequest.reportReviewId())
				.ipAddress(String.valueOf(i))
				.type(ADVERTISEMENT)
				.userId((long) i)
				.reportContent("이 리뷰는 광고 리뷰입니다.")
				.build();
			reviewReportRepository.save(reviewReport);
		});
		Review beforeReview = reviewRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);

		MvcResult authResult = mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(new OauthRequest("test@test.com", SocialType.KAKAO, null, null)))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = authResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		JsonNode dataNode = mapper.convertValue(response.getData(), JsonNode.class);
		String accessToken = dataNode.get("accessToken").asText();

		MvcResult result = mockMvc.perform(post("/api/v1/reports/review")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewReportRequest))
				.header("Authorization", "Bearer " + accessToken)
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		ReviewReport savedReport = reviewReportRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);
		assertNotNull(savedReport);

		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse res = mapper.readValue(content, GlobalResponse.class);
		ReviewReportResponse reviewReportResponse = mapper.convertValue(res.getData(), ReviewReportResponse.class);

		Review afterReview = reviewRepository.findById(reviewReportRequest.reportReviewId()).orElse(null);

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
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_4() throws Exception {
		UserReportRequest userReportRequest = new UserReportRequest(2L, UserReportType.FRAUD, "아주 나쁜놈이에요 신고합니다.");

		MvcResult result = mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(userReportRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		UserReportResponse userReportResponse = mapper.convertValue(response.getData(), UserReportResponse.class);

		assertEquals(userReportResponse.getMessage(), SUCCESS.getMessage());
	}

}
