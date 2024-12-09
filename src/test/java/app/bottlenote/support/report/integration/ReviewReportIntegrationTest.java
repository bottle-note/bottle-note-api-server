package app.bottlenote.support.report.integration;

import static app.bottlenote.review.domain.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.domain.constant.ReviewActiveStatus.DISABLED;
import static app.bottlenote.support.report.domain.constant.ReviewReportType.ADVERTISEMENT;
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
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.response.ReviewReportResponse;
import app.bottlenote.support.report.fixture.ReviewReportFixture;
import app.bottlenote.support.report.repository.ReviewReportRepository;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] HelpController")
@WithMockUser
class ReviewReportIntegrationTest extends IntegrationTestSupport {

	private ReviewReportRequest reviewReportRequest;

	@Autowired
	private ReviewReportRepository reviewReportRepository;
	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		reviewReportRequest = new ReviewReportRequest(1L, ADVERTISEMENT, "이 리뷰는 광고 리뷰입니다.");
	}

	@DisplayName("리뷰를 신고할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_1() throws Exception {
		// given

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
		final Long reviewId = 1L;
		IntStream.range(1, 5).forEach((int i) -> {
			ReviewReport reviewReport = ReviewReportFixture.getReviewReportObject()
				.reviewId(reviewId)
				.ipAddress(String.valueOf(i))
				.type(ADVERTISEMENT)
				.userId((long) i)
				.reportContent("이 리뷰는 광고 리뷰입니다.")
				.build();
			reviewReportRepository.save(reviewReport);
		});
		Review saved = reviewRepository.findById(reviewId).orElse(null);
		assertNotNull(saved);
		assertEquals(ACTIVE, saved.getActiveStatus());
		log.info("누적 신고 5개 전 리뷰 상태: {}", saved.getActiveStatus());

		final Long reviewReportId = 1L;

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

		ReviewReport savedReport = reviewReportRepository.findById(reviewReportId).orElse(null);
		assertNotNull(savedReport);

		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse res = mapper.readValue(content, GlobalResponse.class);
		ReviewReportResponse reviewReportResponse = mapper.convertValue(res.getData(), ReviewReportResponse.class);

		assertTrue(reviewReportResponse.success());

		Review shouldBlockReview = reviewRepository.findById(reviewId).orElse(null);

		assertNotNull(shouldBlockReview);
		assertEquals(DISABLED, shouldBlockReview.getActiveStatus());
		log.info("누적 신고 5개 이후 리뷰 상태: {}", shouldBlockReview.getActiveStatus());
	}

}
