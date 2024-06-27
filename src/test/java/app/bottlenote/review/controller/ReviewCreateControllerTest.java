package app.bottlenote.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("리뷰 등록 컨트롤러 테스트")
@WebMvcTest(ReviewController.class)
@WithMockUser
class ReviewCreateControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ReviewService reviewService;

	private final ReviewCreateRequest request = ReviewObjectFixture.getReviewCreateRequest();
	private final ReviewCreateResponse response = ReviewObjectFixture.getReviewCreateResponse();

	@DisplayName("리뷰를 등록할 수 있다.")
	@Test
	void create_review_test() throws Exception {

		try (MockedStatic<SecurityContextUtil> mockedValidator = mockStatic(
			SecurityContextUtil.class)) {

			mockedValidator.when(SecurityContextUtil::getUserIdByContext)
				.thenReturn(Optional.of(1L));

			when(reviewService.createReview(any(), anyLong()))
				.thenReturn(response);

			mockMvc.perform(post("/api/v1/reviews")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.data.content").value("맛있어요"));
		}
	}

	@DisplayName("리뷰 등록에 실패한다.")
	@Test
	void create_review_fail_test() throws Exception {

		ReviewCreateRequest wrongRequest = new ReviewCreateRequest(
			1L,
			ReviewDisplayStatus.PUBLIC,
			"맛있어요",
			null,
			new BigDecimal("30000.0"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"),
			List.of(
				new ReviewImageInfo(1L, "url1"),
				new ReviewImageInfo(2L, "url2"),
				new ReviewImageInfo(3L, "url3"),
				new ReviewImageInfo(4L, "url4"),
				new ReviewImageInfo(5L, "url5"),
				new ReviewImageInfo(6L, "url6")
			),
			List.of("테이스팅태그", "테이스팅태그 2", "테이스팅태그 3")
		);

		try (MockedStatic<SecurityContextUtil> mockedValidator = mockStatic(
			SecurityContextUtil.class)) {

			mockedValidator.when(SecurityContextUtil::getUserIdByContext)
				.thenReturn(Optional.of(1L));

			when(reviewService.createReview(any(), anyLong()))
				.thenThrow(HttpMessageNotReadableException.class);

			mockMvc.perform(post("/api/v1/reviews")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest());
		}
	}
}
