package app.bottlenote.review.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo.ReviewInfo;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] ReviewReplyController")
@WithMockUser
class ReviewIntegrationTest extends IntegrationTestSupport {

	private OauthRequest oauthRequest;
	private ReviewModifyRequest reviewModifyRequest;
	private ReviewModifyRequest nullableReviewModifyRequest;
	private ReviewModifyRequest wrongReviewModifyRequest;

	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();
		nullableReviewModifyRequest = ReviewObjectFixture.getNullableReviewModifyRequest();
		wrongReviewModifyRequest = ReviewObjectFixture.getWrongReviewModifyRequest();
		oauthRequest = new OauthRequest("chadongmin@naver.com", SocialType.KAKAO, null, null);
	}

	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
		"/init-script/init-user.sql",
		"/init-script/init-review.sql",
		"/init-script/init-review-reply.sql"}
	)
	@Nested
	@DisplayName("[Integration] 리뷰 목록 조회 통합테스트")
	class ReviewReadIntegrationTest extends IntegrationTestSupport {

		@Autowired
		AlcoholQueryRepository alcoholQueryRepository;

		@DisplayName("베스트 리뷰 여부가 정확히 동작한다.")
		@Test
		void test_1() throws Exception {
			// given
			Long alcoholId = 4L;

			MvcResult result = mockMvc.perform(get("/api/v1/alcohols/{alcoholId}", alcoholId)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			// when
			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			AlcoholDetail alcoholDetail = mapper.convertValue(response.getData(), AlcoholDetail.class);
			List<ReviewInfo> bestReviewInfos = alcoholDetail.reviewList().getBestReviewInfos();
			Long bestReviewIdInAlcohol = bestReviewInfos.get(0).reviewId();

			MvcResult result2 = mockMvc.perform(get("/api/v1/reviews/{alcoholId}", alcoholId)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString2 = result2.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response2 = mapper.readValue(contentAsString2, GlobalResponse.class);
			ReviewListResponse reviewListResponse = mapper.convertValue(response2.getData(), ReviewListResponse.class);

			Long bestReviewIdInReview = reviewListResponse.reviewList().stream()
				.filter(bestReview -> bestReview.isBestReview().equals(true))
				.toList().get(0).reviewId();

			// then
			Assertions.assertEquals(bestReviewIdInReview, bestReviewIdInAlcohol);
		}
	}

	@Nested
	@DisplayName("[Integration] 리뷰 수정 통합테스트")
	@WithMockUser
	class ReviewModifyIntegrationTest extends IntegrationTestSupport {

		@BeforeEach
		void setUp() {
			reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();
			nullableReviewModifyRequest = ReviewObjectFixture.getNullableReviewModifyRequest();
			wrongReviewModifyRequest = ReviewObjectFixture.getWrongReviewModifyRequest();
			oauthRequest = new OauthRequest("chadongmin@naver.com", SocialType.KAKAO, null, null);

		}


		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("리뷰 수정에 성공한다.")
		@Test
		void test_1() throws Exception {
			log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

			final Long reviewId = 1L;

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewModifyRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			Review savedReview = reviewRepository.findById(reviewId).orElseGet(null);

			assertEquals(savedReview.getContent(), reviewModifyRequest.content());
		}

		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("content와 status를 제외한 필드에 null이 할당되어도 수정에 성공한다.")
		@Test
		void test_2() throws Exception {
			log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

			final Long reviewId = 1L;

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(nullableReviewModifyRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			Review savedReview = reviewRepository.findById(reviewId).orElseGet(null);

			assertEquals(savedReview.getContent(), nullableReviewModifyRequest.content());
		}

		@DisplayName("Not Null인 필드에 null이 할당되면 리뷰 수정에 실패한다.")
		@Test
		void test_3() throws Exception {
			log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

			final Long reviewId = 1L;

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongReviewModifyRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();
		}
	}

}
