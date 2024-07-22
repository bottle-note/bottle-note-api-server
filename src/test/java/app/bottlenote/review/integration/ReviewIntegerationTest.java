package app.bottlenote.review.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

@DisplayName("[Integration] 리뷰 통합 테스트")
class ReviewIntegerationTest extends IntegrationTestSupport {

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;
	private ReviewModifyRequest reviewModifyRequest;
	private ReviewModifyRequest nullableReviewModifyRequest;
	private ReviewModifyRequest wrongReviewModifyRequest;

	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();
		nullableReviewModifyRequest = ReviewObjectFixture.getNullableReviewModifyRequest();
		wrongReviewModifyRequest = ReviewObjectFixture.getWrongReviewModifyRequest();
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
		"/init-script/init-user.sql",
		"/init-script/init-review.sql",
		"/init-script/init-review-reply.sql"}
	)

	@Nested
	@DisplayName("[Integration] 리뷰 수정 통합테스트")
	@WithMockUser
	class ReviewModifyIntegrationTest extends IntegrationTestSupport {

		@DisplayName("리뷰 수정에 성공한다.")
		@Test
		void test_1() throws Exception {
			log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

			final Long reviewId = 1L;
			final Long userId = 2L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewModifyRequest))
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

		@DisplayName("content와 status를 제외한 필드에 null이 할당되어도 수정에 성공한다.")
		@Test
		void test_2() throws Exception {
			log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());

			final Long reviewId = 1L;
			final Long userId = 2L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(nullableReviewModifyRequest))
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
			final Long userId = 2L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongReviewModifyRequest))
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
