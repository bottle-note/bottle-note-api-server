package app.bottlenote.review.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.dto.response.ReviewResultMessage;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
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
@DisplayName("리뷰 삭제 컨트롤러 테스트")
@WebMvcTest(ReviewController.class)
class ReviewDeleteControllerTest {


	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ReviewService reviewService;

	private final Long reviewId = 1L;
	private final Long userId = 1L;
	private final ReviewResultResponse response = ReviewResultResponse.response(ReviewResultMessage.DELETE_SUCCESS, reviewId);
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("리뷰를 삭제할 수 있다")
	@Test
	void delete_review_success() throws Exception {

		String codeMessage = "DELETE_SUCCESS";
		String message = "리뷰 삭제가 성공적으로 완료되었습니다.";

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.deleteReview(reviewId, userId)).thenReturn(response);

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.codeMessage").value(codeMessage))
			.andExpect(jsonPath("$.data.message").value(message))
			.andExpect(jsonPath("$.data.reviewId").value(reviewId));

		verify(reviewService, description("deleteReview 메서드가 정상적으로 호출됨"))
			.deleteReview(anyLong(), anyLong());
	}

	@DisplayName("로그인하지 않은 유저는 리뷰를 삭제할 수 없다.")
	@Test
	void delete_review_fail_unauthorized_user() throws Exception {

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());

		when(reviewService.deleteReview(reviewId, userId)).thenReturn(response);

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@DisplayName("존재하지 않은 리뷰를 삭제할 수 없다.")
	@Test
	void delete_review_fail_not_exist_review() throws Exception {

		Long reviewId = 1L;

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.deleteReview(reviewId, userId))
			.thenThrow(new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isBadRequest())
			.andDo(print());

		verify(reviewService, description("deleteReview 메서드가 정상적으로 호출됨"))
			.deleteReview(anyLong(), anyLong());
	}
}


