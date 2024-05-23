package app.bottlenote.review.controller;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser()
@DisplayName("리뷰 조회 컨트롤러 테스트")
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ReviewService reviewService;

	static Stream<Arguments> testCase1Provider() {
		return Stream.of(
			Arguments.of("모든 요청 파라미터가 존재할 때.",
				PageableRequest.builder()
					.sortType(ReviewSortType.POPULAR)
					.sortOrder(SortOrder.DESC)
					.cursor(1L)
					.pageSize(2L)
					.build()
			),
			Arguments.of("정렬 정보가 없을 때.",
				PageableRequest.builder()
					.sortType(null)
					.sortOrder(null)
					.cursor(0L)
					.pageSize(3L)
					.build()
			),
			Arguments.of("페이지네이션 정보가 없을 때.",
				PageableRequest.builder()
					.sortType(null)
					.sortOrder(null)
					.cursor(null)
					.pageSize(null)
					.build()
			)
		);
	}

	static Stream<Arguments> sortOrderParameters() {
		return Stream.of(
			// 성공 케이스
			Arguments.of("ASC", 200),
			Arguments.of("DESC", 200),
			// 실패 케이스
			Arguments.of("DESCCC", 400),
			Arguments.of("ASCC", 400)
		);
	}

	static Stream<Arguments> sortTypeParameters() {
		return Stream.of(
			// 성공 케이스

			Arguments.of("POPULAR", 200),
			Arguments.of("RATING", 200),
			// 실패 케이스

			Arguments.of("Popular", 400),
			Arguments.of("Rating", 400)
		);
	}

	@DisplayName("리뷰를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}]{0}")
	@MethodSource("testCase1Provider")
	void test_case_1(String description, PageableRequest pageableRequest) throws Exception {

		//given
		PageResponse<ReviewResponse> response = getResponse();

		//when
		when(reviewService.getReviews(any(), any(), any()))
			.thenReturn(response);

		ResultActions resultActions = mockMvc.perform(get("/api/v1/reviews/1")
				.param("sortType", String.valueOf(pageableRequest.sortType()))
				.param("sortOrder", pageableRequest.sortOrder().name())
				.param("cursor", String.valueOf(pageableRequest.cursor()))
				.param("pageSize", String.valueOf(pageableRequest.pageSize()))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(2));
		resultActions.andExpect(jsonPath("$.data.reviewList[0].reviewId").value(1));
		resultActions.andExpect(jsonPath("$.data.reviewList[0].reviewContent").value("맛있습니다"));
		//resultActions.andExpect(jsonPath("$.data.reviewList[0].engName").value("anCnoc 24-year-old"));

	}

	private PageResponse<ReviewResponse> getResponse() {

		ReviewDetail reviewDetail_1 = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent("맛있습니다")
			.price(BigDecimal.valueOf(100000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(5L)
			.replyCount(3L)
			.thumbnailImage("thumbnail_image_1")
			.reviewCreatedAt(LocalDateTime.now())
			.userId(1L)
			.userNickname("test_user_1")
			.userProfileImage("user_profile_image_1")
			.ratingPoint(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasCommentedByMe(false)
			.build();

		ReviewDetail reviewDetail_2 = ReviewDetail.builder()
			.reviewId(2L)
			.reviewContent("나름 먹을만 하네요")
			.price(BigDecimal.valueOf(110000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(3L)
			.replyCount(6L)
			.thumbnailImage("thumbnail_image_2")
			.reviewCreatedAt(LocalDateTime.now().minusDays(1))
			.userId(2L)
			.userNickname("test_user_2")
			.userProfileImage("user_profile_image_2")
			.ratingPoint(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasCommentedByMe(false)
			.build();

		Long totalCount = 2L;
		List<ReviewDetail> reviewDetails = List.of(reviewDetail_1, reviewDetail_2);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(1L)
			.pageSize(2L)
			.hasNext(false)
			.build();

		ReviewResponse response = ReviewResponse.of(totalCount, reviewDetails);
		return PageResponse.of(response, cursorPageable);
	}

	@DisplayName("정렬 타입에 대한 검증")
	@ParameterizedTest(name = "{1} : {0}")
	@MethodSource("sortTypeParameters")
	void test_sortType(String sortType, int expectedStatus) throws Exception {
		// given
		PageResponse<ReviewResponse> response = getResponse();

		// when
		when(reviewService.getReviews(any(), any(), any())).thenReturn(response);

		mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/reviews/1")
				.param("keyword", "")
				.param("category", "")
				.param("regionId", "")
				.param("sortType", sortType)
				.param("sortOrder", "DESC")
				.param("cursor", "")
				.param("pageSize", "")
				.with(csrf())
			)
			.andExpect(status().is(expectedStatus))
			.andDo(print());
	}

	@DisplayName("정렬 방향에 대한 검증")
	@ParameterizedTest(name = "{1} : {0}")
	@MethodSource("sortOrderParameters")
	void test_sortOrder(String sortOrder, int expectedStatus) throws Exception {
		// given
		PageResponse<ReviewResponse> response = getResponse();

		// when
		when(reviewService.getReviews(any(), any(), any())).thenReturn(response);

		mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/reviews/1")
				.param("category", "")
				.param("regionId", "")
				.param("sortType", "POPULAR")
				.param("sortOrder", sortOrder)
				.param("cursor", "")
				.param("pageSize", "")
				.with(csrf())
			)
			.andExpect(status().is(expectedStatus))
			.andDo(print());
	}


}
