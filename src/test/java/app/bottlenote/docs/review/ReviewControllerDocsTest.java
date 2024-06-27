package app.bottlenote.docs.review;

import static app.bottlenote.review.dto.response.ReviewResultMessage.DELETE_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.controller.ReviewController;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@DisplayName("리뷰 컨트롤러 RestDocs용 테스트")
class ReviewControllerDocsTest extends AbstractRestDocs {

	private final Long userId = 1L;

	private final ReviewService reviewService = mock(ReviewService.class);
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	@Override
	protected Object initController() {
		return new ReviewController(reviewService);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Test
	@DisplayName("리뷰를 등록할 수 있다.")
	void review_create_test() throws Exception {

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.createReview(any(), anyLong()))
			.thenReturn(ReviewObjectFixture.getReviewCreateResponse());

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ReviewObjectFixture.getReviewCreateRequest()))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-create",
					requestFields(
						fieldWithPath("alcoholId").type(NUMBER).description("술 ID"),
						fieldWithPath("content").type(STRING).description("리뷰 내용"),
						fieldWithPath("status").type(STRING).description("리뷰 상태"),
						fieldWithPath("price").type(NUMBER).description("가격"),
						fieldWithPath("sizeType").type(STRING).description("술 타입 (잔 or 병"),
						fieldWithPath("locationInfo").type(OBJECT).description("위치 정보"),
						fieldWithPath("locationInfo.zipCode").type(STRING).description("우편번호"),
						fieldWithPath("locationInfo.address").type(STRING).description("주소"),
						fieldWithPath("locationInfo.detailAddress").type(STRING).description("상세 주소"),
						fieldWithPath("imageUrlList").type(ARRAY).description("이미지 URL 목록"),
						fieldWithPath("imageUrlList[].order").type(NUMBER).description("이미지 순서"),
						fieldWithPath("imageUrlList[].viewUrl").type(STRING).description("이미지 뷰 URL"),
						fieldWithPath("tastingTagList[]").type(ARRAY).description("테이스팅 태그 목록")
					),
					responseFields(
						fieldWithPath("success").type(BOOLEAN).description("요청 성공 여부"),
						fieldWithPath("code").type(NUMBER).description("응답 코드"),
						fieldWithPath("data").type(OBJECT).description("응답 데이터"),
						fieldWithPath("data.id").type(NUMBER).description("생성된 리뷰 ID"),
						fieldWithPath("data.content").type(STRING).description("리뷰 내용"),
						fieldWithPath("data.callback").type(STRING).description("콜백 URL"),
						fieldWithPath("errors").type(ARRAY).description("에러 목록"),
						fieldWithPath("meta").type(OBJECT).description("메타 정보"),
						fieldWithPath("meta.serverVersion").type(STRING).description("서버 버전"),
						fieldWithPath("meta.serverEncoding").type(STRING).description("서버 인코딩"),
						fieldWithPath("meta.serverResponseTime").type(STRING).description("서버 응답 시간"),
						fieldWithPath("meta.serverPathVersion").type(STRING).description("서버 경로 버전")
					)
				)
			);
	}


	@Test
	@DisplayName("리뷰를 조회할 수 있다.")
	void review_read_test() throws Exception {

		//given
		PageResponse<ReviewResponse> response = ReviewObjectFixture.getReviewListResponse();

		//when
		when(reviewService.getReviews(any(), any(), any())).thenReturn(
			response);

		//then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reviews/1")
				.param("sortType", "POPULAR")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "2")
			)
			.andExpect(status().isOk())
			.andDo(
				document("review/review-read",
					queryParameters(
						parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
						parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
						parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
						parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.totalCount").description("전체 리뷰 리스트의 크기"),
						fieldWithPath("data.reviewList[].reviewId").description("리뷰 ID"),
						fieldWithPath("data.reviewList[].reviewContent").description("리뷰 내용"),
						fieldWithPath("data.reviewList[].price").description("가격"),
						fieldWithPath("data.reviewList[].sizeType").description("사이즈 타입 (BOTTLE, GLASS)"),
						fieldWithPath("data.reviewList[].likeCount").description("좋아요 개수"),
						fieldWithPath("data.reviewList[].replyCount").description("댓글 개수"),
						fieldWithPath("data.reviewList[].reviewImageUrl").description("리뷰 썸네일 이미지"),
						fieldWithPath("data.reviewList[].createAt").description("리뷰 등록 일시"),
						fieldWithPath("data.reviewList[].userId").description("유저 ID"),
						fieldWithPath("data.reviewList[].nickName").description("유저 닉네임"),
						fieldWithPath("data.reviewList[].zipCode").description("(업장)우편번호"),
						fieldWithPath("data.reviewList[].address").description("(업장)주소"),
						fieldWithPath("data.reviewList[].detailAddress").description("(업장)상세주소"),
						fieldWithPath("data.reviewList[].userProfileImage").description("유저 프로필 이미지"),
						fieldWithPath("data.reviewList[].rating").description("리뷰에 등록된 별점"),
						fieldWithPath("data.reviewList[].status").description("리뷰 공개 상태 (PUBLIC, PRIVATE)"),
						fieldWithPath("data.reviewList[].isMyReview").description("내가 작성한 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].isLikedByMe").description("내가 좋아요 누른 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].hasReplyByMe").description("내가 댓글을 단 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].reviewTastingTag").description("테이스팅 태그 리스트"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.pageable").description("페이징 정보"),
						fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
						fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
						fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
						fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부")
					)
				)
			);
	}

	@Test
	@DisplayName("내가 작성한 리뷰를 조회할 수 있다.")
	void my_review_read_test() throws Exception {

		//given
		PageResponse<ReviewResponse> response = ReviewObjectFixture.getReviewListResponse();

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.getMyReviews(any(), any(), any())).thenReturn(
			response);

		//then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reviews/me/1")
				.param("sortType", "POPULAR")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "2")
			)
			.andExpect(status().isOk())
			.andDo(
				document("review/my-review-read",
					queryParameters(
						parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
						parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
						parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
						parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.totalCount").description("전체 리뷰 리스트의 크기"),
						fieldWithPath("data.reviewList[].reviewId").description("리뷰 ID"),
						fieldWithPath("data.reviewList[].reviewContent").description("리뷰 내용"),
						fieldWithPath("data.reviewList[].price").description("가격"),
						fieldWithPath("data.reviewList[].sizeType").description("사이즈 타입 (BOTTLE, GLASS)"),
						fieldWithPath("data.reviewList[].likeCount").description("좋아요 개수"),
						fieldWithPath("data.reviewList[].replyCount").description("댓글 개수"),
						fieldWithPath("data.reviewList[].reviewImageUrl").description("리뷰 썸네일 이미지"),
						fieldWithPath("data.reviewList[].createAt").description("리뷰 등록 일시"),
						fieldWithPath("data.reviewList[].userId").description("유저 ID"),
						fieldWithPath("data.reviewList[].nickName").description("유저 닉네임"),
						fieldWithPath("data.reviewList[].zipCode").description("(업장)우편번호"),
						fieldWithPath("data.reviewList[].address").description("(업장)주소"),
						fieldWithPath("data.reviewList[].detailAddress").description("(업장)상세주소"),
						fieldWithPath("data.reviewList[].userProfileImage").description("유저 프로필 이미지"),
						fieldWithPath("data.reviewList[].rating").description("리뷰에 등록된 별점"),
						fieldWithPath("data.reviewList[].status").description("리뷰 공개 상태 (PUBLIC, PRIVATE)"),
						fieldWithPath("data.reviewList[].isMyReview").description("내가 작성한 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].isLikedByMe").description("내가 좋아요 누른 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].hasReplyByMe").description("내가 댓글을 단 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].reviewTastingTag").description("테이스팅 태그 리스트"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.pageable").description("페이징 정보"),
						fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
						fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
						fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
						fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부")
					)
				)
			);
	}

	@Test
	@DisplayName("리뷰를 수정할 수 있다.")
	void review_modify_test() throws Exception {

		Long reviewId = 1L;

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.modifyReview(any(ReviewModifyRequest.class), any(), any())).thenReturn("리뷰 수정이 성공적으로 완료되었습니다.");

		//then
		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ReviewObjectFixture.getReviewModifyRequest()))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-modify",
					requestFields(
						fieldWithPath("content").type(STRING).description("리뷰 내용").optional(),
						fieldWithPath("status").type(STRING).description("리뷰 상태").optional(),
						fieldWithPath("price").type(NUMBER).description("가격").optional(),
						fieldWithPath("sizeType").type(STRING).description("술 타입 (잔 or 병)").optional(),
						fieldWithPath("locationInfo").type(OBJECT).description("위치 정보").optional(),
						fieldWithPath("locationInfo.zipCode").type(STRING).description("우편번호").optional(),
						fieldWithPath("locationInfo.address").type(STRING).description("주소").optional(),
						fieldWithPath("locationInfo.detailAddress").type(STRING).description("상세 주소").optional(),
						fieldWithPath("imageUrlList").type(ARRAY).description("이미지 URL 목록"),
						fieldWithPath("imageUrlList[].order").type(NUMBER).description("이미지 순서"),
						fieldWithPath("imageUrlList[].viewUrl").type(STRING).description("이미지 뷰 URL"),
						fieldWithPath("tastingTagList").type(ARRAY).description("테이스팅 태그 목록").optional()
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data").description("성공 메시지"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

	@Test
	@DisplayName("리뷰를 삭제할 수 있다.")
	void review_delete_test() throws Exception {

		Long reviewId = 1L;

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.deleteReview(anyLong(), anyLong())).thenReturn(ReviewResultResponse.response(DELETE_SUCCESS, reviewId));

		//then
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-delete",
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
						fieldWithPath("data.message").description("성공 메시지"),
						fieldWithPath("data.reviewId").description("리뷰 아이디"),
						fieldWithPath("data.responseAt").description("서버 응답 일시"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}
}
