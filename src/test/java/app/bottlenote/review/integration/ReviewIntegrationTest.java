package app.bottlenote.review.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] ReviewReplyController")
class ReviewIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private ReviewRepository reviewRepository;

	@Nested
	@DisplayName("리뷰 목록 조회 테스트")
	class select {
		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("리뷰 목록 조회에 성공한다.")
		@Test
		void test_1() throws Exception {
			// given
			Long alcoholId = 4L;

			//when
			MvcResult result = mockMvc.perform(get("/api/v1/reviews/{alcoholId}", alcoholId)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			ReviewListResponse reviewListResponse = mapper.convertValue(response.getData(), ReviewListResponse.class);
			List<ReviewInfo> reviewInfos = reviewListResponse.reviewList();

			//when
			assertNotNull(reviewListResponse);
			assertFalse(reviewInfos.isEmpty());
		}
	}

	@Nested
	@DisplayName("리뷰 생성 테스트")
	class create {
	}

	@Nested
	@DisplayName("리뷰 삭제 테스트")
	class delete {
	}

	@Nested
	@DisplayName("리뷰 수정 테스트")
	class update {

		@BeforeEach
		void setUp() {
			final Long tokenUserId = getTokenUserId();
			Review review = ReviewObjectFixture.getReviewFixture(1L, tokenUserId, "content1");
			reviewRepository.save(review);
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
			final Long tokenUserId = getTokenUserId();
			final Long reviewId = reviewRepository.findByUserId(tokenUserId).get(0).getId();
			final ReviewModifyRequest request = ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PUBLIC);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.header("Authorization", "Bearer " + getToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();
			Review savedReview = reviewRepository.findById(reviewId).orElseThrow();
			assertEquals(savedReview.getContent(), request.content());
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
			final Long tokenUserId = getTokenUserId();
			final Long reviewId = reviewRepository.findByUserId(tokenUserId).get(0).getId();
			final ReviewModifyRequest request = ReviewObjectFixture.getNullableReviewModifyRequest(ReviewDisplayStatus.PRIVATE);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.header("Authorization", "Bearer " + getToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			Review savedReview = reviewRepository.findById(reviewId).orElseThrow();

			assertEquals(savedReview.getContent(), request.content());
		}

		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("Not Null인 필드에 null이 할당되면 리뷰 수정에 실패한다.")
		@Test
		void test_3() throws Exception {
			final Error notNullEmpty = Error.of(ValidExceptionCode.REVIEW_CONTENT_REQUIRED);
			final Error notStatusEmpty = Error.of(ValidExceptionCode.REVIEW_DISPLAY_STATUS_NOT_EMPTY);

			final Long tokenUserId = getTokenUserId();
			final Long reviewId = reviewRepository.findByUserId(tokenUserId).get(0).getId();
			final ReviewModifyRequest request = ReviewObjectFixture.getWrongReviewModifyRequest();

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.header("Authorization", "Bearer " + getToken())
					.with(csrf())
				)
				.andExpect(status().isBadRequest()).andDo(print())
				.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_CONTENT_REQUIRED')].status").value(notNullEmpty.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_CONTENT_REQUIRED')].message").value(notNullEmpty.message()))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_DISPLAY_STATUS_NOT_EMPTY')].status").value(notStatusEmpty.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_DISPLAY_STATUS_NOT_EMPTY')].message").value(notStatusEmpty.message()))
				.andReturn();

		}

		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("리뷰 상태 변경에 성공한다.")
		@Test
		void test_4() throws Exception {

			final Long tokenUserId = getTokenUserId();
			final Long reviewId = reviewRepository.findByUserId(tokenUserId).get(0).getId();
			final ReviewModifyRequest request = ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PRIVATE);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}/display", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.header("Authorization", "Bearer " + getToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();
			Review savedReview = reviewRepository.findById(reviewId).orElseThrow();
			assertEquals(ReviewDisplayStatus.PRIVATE, savedReview.getStatus());
		}

		@Sql(scripts = {
			"/init-script/init-alcohol.sql",
			"/init-script/init-user.sql",
			"/init-script/init-review.sql",
			"/init-script/init-review-reply.sql"}
		)
		@DisplayName("Not null인 필드에 null이 할당되면 리뷰 상태 변경에 실패한다.")
		@Test
		void test_5() throws Exception {
			final Error notStatusEmpty = Error.of(ValidExceptionCode.REVIEW_DISPLAY_STATUS_NOT_EMPTY);
			final ReviewModifyRequest request = ReviewObjectFixture.getNullableReviewModifyRequest(null);

			final Long tokenUserId = getTokenUserId();
			final Long reviewId = reviewRepository.findByUserId(tokenUserId).get(0).getId();

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}/display", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.header("Authorization", "Bearer " + getToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_DISPLAY_STATUS_NOT_EMPTY')].status").value(notStatusEmpty.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'REVIEW_DISPLAY_STATUS_NOT_EMPTY')].message").value(notStatusEmpty.message()))
				.andReturn();
		}
	}

	@Nested
	@DisplayName("리뷰 상태 변경")
	class changeStatus {
	}

}
