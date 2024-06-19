package app.bottlenote.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser()
@DisplayName("리뷰 수정 컨트롤러 테스트")
@WebMvcTest(ReviewController.class)
class ReviewModifyControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ReviewService reviewService;

	private User user;

	private ReviewModifyRequest reviewModifyRequest;

	private ReviewDetail reviewDetail;

	private final Long userId = 1L;

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	@BeforeEach
	void setup() {
		user = User.builder().id(userId).build();

		reviewModifyRequest = new ReviewModifyRequest(
			"별로에요",
			null,
			null,
			null,
			null,
			null);

		reviewDetail = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent(reviewModifyRequest.content())
			.build();
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("리뷰를 수정할 수 있다.")
	@Test
	void modify_review_success() throws Exception {

		Long reviewId = 99L;

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.modifyReviews(reviewModifyRequest, reviewId, user.getId()))
			.thenReturn(reviewDetail);

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewModifyRequest))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());

		verify(reviewService, description("modifyReviews 메서드가 정상적으로 호출됨"))
			.modifyReviews(any(ReviewModifyRequest.class), anyLong(), anyLong());

	}

	@DisplayName("로그인하지 않은 유저는 리뷰를 수정할 수 없다.")
	@Test
	void modify_review_fail_unauthorized_user() throws Exception {

		Long reviewId = 1L;

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());

		when(reviewService.modifyReviews(reviewModifyRequest, reviewId, user.getId()))
			.thenReturn(reviewDetail);

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewModifyRequest))
				.with(csrf())
			)
			.andExpect(status().isBadRequest())
			.andDo(print());

		// modifyReview 메서드가 호출되지 않음
		verify(reviewService, never()).modifyReviews(any(ReviewModifyRequest.class), anyLong(), anyLong());
	}

	@DisplayName("존재하지 않은 리뷰를 수정할 수 없다.")
	@Test
	void modify_review_fail_not_exist_review() throws Exception {

		Long reviewId = 1L;

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.modifyReviews(reviewModifyRequest, reviewId, user.getId()))
			.thenThrow(new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(reviewModifyRequest))
				.with(csrf())
			)
			.andExpect(status().isBadRequest())
			.andDo(print());

		verify(reviewService, description("modifyReviews 메서드가 정상적으로 호출됨"))
			.modifyReviews(any(ReviewModifyRequest.class), anyLong(), anyLong());
	}

}
