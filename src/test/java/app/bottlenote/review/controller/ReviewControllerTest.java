package app.bottlenote.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.security.jwt.CustomJwtException;
import app.bottlenote.global.security.jwt.CustomJwtExceptionCode;
import app.bottlenote.global.security.jwt.JwtExceptionType;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultMessage;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("리뷰 컨트롤러 테스트")
@WebMvcTest(ReviewController.class)
@WithMockUser
class ReviewControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ReviewService reviewService;
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	private final ReviewCreateRequest reviewCreateRequest = ReviewObjectFixture.getReviewCreateRequest();
	private final ReviewCreateResponse reviewCreateResponse = ReviewObjectFixture.getReviewCreateResponse();

	private final ReviewModifyRequest reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();

	private final PageResponse<ReviewListResponse> reviewListResponse = ReviewObjectFixture.getReviewListResponse();


	private final Long reviewId = 1L;
	private final Long userId = 1L;

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Nested
	@DisplayName("리뷰 등록 컨트롤러 테스트")
	class ReviewCreateControllerTest {

		@DisplayName("리뷰를 등록할 수 있다.")
		@Test
		void create_review_test() throws Exception {

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.createReview(any(), anyLong()))
				.thenReturn(reviewCreateResponse);

			mockMvc.perform(post("/api/v1/reviews")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewCreateRequest))
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.data.content").value("맛있어요"));
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

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(1L));

			when(reviewService.createReview(any(), anyLong()))
				.thenThrow(HttpMessageNotReadableException.class);

			mockMvc.perform(post("/api/v1/reviews")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("리뷰 조회 컨트롤러 테스트")
	class ReviewReadControllerTest {

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

			//when
			when(reviewService.getReviews(any(), any(), any()))
				.thenReturn(reviewListResponse);

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
			resultActions.andExpect(jsonPath("$.data.reviewList[0].reviewContent").value("맛있어요"));

		}

		@DisplayName("정렬 타입에 대한 검증")
		@ParameterizedTest(name = "{1} : {0}")
		@MethodSource("sortTypeParameters")
		void test_sortType(String sortType, int expectedStatus) throws Exception {
			// given

			// when
			when(reviewService.getReviews(any(), any(), any())).thenReturn(reviewListResponse);

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

			// when
			when(reviewService.getReviews(any(), any(), any())).thenReturn(reviewListResponse);

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

		@DisplayName("리뷰를 조회할 수 있다.")
		@ParameterizedTest(name = "[{index}]{0}")
		@MethodSource("testCase1Provider")
		void my_review_read_success(String description, PageableRequest pageableRequest) throws Exception {

			//given

			//when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
			when(reviewService.getMyReviews(any(), any(), any()))
				.thenReturn(reviewListResponse);

			ResultActions resultActions = mockMvc.perform(get("/api/v1/reviews/me/1")
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
			resultActions.andExpect(jsonPath("$.data.reviewList[0].reviewContent").value("맛있어요"));
		}

		@Test
		@DisplayName("유저 정보가 없을 경우에는 예외를 반환한다..")
		void test_fail_when_no_auth_info() throws Exception {

			//given

			//when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());
			when(reviewService.getMyReviews(any(), any(), any()))
				.thenReturn(reviewListResponse);

			mockMvc.perform(get("/api/v1/reviews/me/1")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf())
				)
				.andExpect(status().isBadRequest())
				.andDo(print());
		}

		@Test
		@DisplayName("Authorization Header가 Null일 경우에는 예외를 반환한다.")
		void test_fail_when_authorization_header_is_null() throws Exception {

			//when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.getMyReviews(any(), any(), any()))
				.thenThrow(new CustomJwtException(CustomJwtExceptionCode.EMPTY_JWT_TOKEN));

			// then
			mockMvc.perform(get("/api/v1/reviews/me/1")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.message").value(CustomJwtExceptionCode.EMPTY_JWT_TOKEN.getMessage()))
				.andDo(print());
		}

		@Test
		@DisplayName("토큰이 잘못된 토큰일 경우 예외를 반환한다.")
		void test_fail_when_token_is_wrong() throws Exception {

			//when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.getMyReviews(any(), any(), any()))
				.thenThrow(new MalformedJwtException(JwtExceptionType.MALFORMED_TOKEN.getMessage()));

			// then
			mockMvc.perform(get("/api/v1/reviews/me/1")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors").value(JwtExceptionType.MALFORMED_TOKEN.getMessage()))
				.andDo(print());
		}

		@Test
		@DisplayName("토큰이 만료 된 토큰일 경우 예외를 반환한다.")
		void test_fail_when_token_is_expired() throws Exception {

			//when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.getMyReviews(any(), any(), any()))
				.thenThrow(new ExpiredJwtException(null, null, JwtExceptionType.EXPIRED_TOKEN.getMessage()));

			// then
			mockMvc.perform(get("/api/v1/reviews/me/1")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errors").value(JwtExceptionType.EXPIRED_TOKEN.getMessage()))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("리뷰 수정 컨트롤러 테스트")
	class ReviewModifyControllerTest {

		private final String response = ReviewResultMessage.MODIFY_SUCCESS.getDescription();

		@DisplayName("리뷰를 수정할 수 있다.")
		@Test
		void modify_review_success() throws Exception {

			Long reviewId = 99L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.modifyReview(reviewModifyRequest, reviewId, userId))
				.thenReturn(response);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewModifyRequest))
					.with(csrf())
				)
				.andExpect(status().isOk())
				.andDo(print());

			verify(reviewService, description("modifyReviews 메서드가 정상적으로 호출됨"))
				.modifyReview(any(ReviewModifyRequest.class), anyLong(), anyLong());

		}

		@DisplayName("로그인하지 않은 유저는 리뷰를 수정할 수 없다.")
		@Test
		void modify_review_fail_unauthorized_user() throws Exception {

			Long reviewId = 1L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());

			when(reviewService.modifyReview(reviewModifyRequest, reviewId, userId))
				.thenReturn(response);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewModifyRequest))
					.with(csrf())
				)
				.andExpect(status().isBadRequest())
				.andDo(print());

			// modifyReview 메서드가 호출되지 않음
			verify(reviewService, never()).modifyReview(any(ReviewModifyRequest.class), anyLong(), anyLong());
		}

		@DisplayName("존재하지 않은 리뷰를 수정할 수 없다.")
		@Test
		void modify_review_fail_not_exist_review() throws Exception {

			Long reviewId = 1L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.modifyReview(reviewModifyRequest, reviewId, userId))
				.thenThrow(new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(reviewModifyRequest))
					.with(csrf())
				)
				.andExpect(status().isBadRequest())
				.andDo(print());

			verify(reviewService, description("modifyReviews 메서드가 정상적으로 호출됨"))
				.modifyReview(any(ReviewModifyRequest.class), anyLong(), anyLong());
		}

		@DisplayName("Request Body에 Null인 필드가 포함되면 리뷰를 수정할 수 없다..")
		@Test
		void modify_review_fail_when_request_body_has_null() throws Exception {

			ReviewModifyRequest wrongRequest = new ReviewModifyRequest(
				"그저 그래요",
				ReviewDisplayStatus.PUBLIC,
				BigDecimal.valueOf(10000L),
				null,
				SizeType.GLASS,
				List.of(),
				new LocationInfo(null, null, null));
			Long reviewId = 99L;

			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			when(reviewService.modifyReview(wrongRequest, reviewId, userId))
				.thenReturn(response);

			mockMvc.perform(patch("/api/v1/reviews/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongRequest))
					.with(csrf())
				)
				.andExpect(status().isBadRequest())
				.andDo(print());

			verify(reviewService, never())
				.modifyReview(any(ReviewModifyRequest.class), anyLong(), anyLong());
		}
	}

	@Nested
	@DisplayName("리뷰 삭제 컨트롤러 테스트")
	class ReviewDeleteControllerTest {

		private final ReviewResultResponse response = ReviewResultResponse.response(ReviewResultMessage.DELETE_SUCCESS, reviewId);

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
}
