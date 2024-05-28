package app.bottlenote.docs.review;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.controller.ReviewController;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("리뷰 컨트롤러 RestDocs용 테스트")
class ReviewControllerDocsTest extends AbstractRestDocs {

	private final ReviewService reviewService = mock(ReviewService.class);

	@Override
	protected Object initController() {
		return new ReviewController(reviewService);
	}

	@Test
	@DisplayName("리뷰를 조회할 수 있다.")
	void review_read_test() throws Exception {

		//given
		PageableRequest request = getRequest();
		PageResponse<ReviewResponse> response = getResponse();

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
						fieldWithPath("data.reviewList[].userProfileImage").description("유저 프로필 이미지"),
						fieldWithPath("data.reviewList[].rating").description("리뷰에 등록된 별점"),
						fieldWithPath("data.reviewList[].status").description("리뷰 공개 상태 (PUBLIC, PRIVATE)"),
						fieldWithPath("data.reviewList[].isMyReview").description("내가 작성한 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].isLikedByMe").description("내가 좋아요 누른 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].hasReplyByMe").description("내가 댓글을 단 리뷰인지 여부"),
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

	private PageableRequest getRequest() {
		return PageableRequest.builder()
			.sortType(ReviewSortType.POPULAR)
			.sortOrder(SortOrder.DESC)
			.cursor(0L)
			.pageSize(2L)
			.build();
	}

	private PageResponse<ReviewResponse> getResponse() {
		ReviewDetail reviewDetail_1 = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent("맛있습니다")
			.price(BigDecimal.valueOf(100000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(5L)
			.replyCount(3L)
			.reviewImageUrl("https://picsum.photos/600/600")
			.createAt(LocalDateTime.now())
			.userId(1L)
			.nickName("test_user_1")
			.userProfileImage("user_profile_image_1")
			.rating(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
			.build();

		ReviewDetail reviewDetail_2 = ReviewDetail.builder()
			.reviewId(2L)
			.reviewContent("나름 먹을만 하네요")
			.price(BigDecimal.valueOf(110000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(3L)
			.replyCount(6L)
			.reviewImageUrl("https://picsum.photos/600/600")
			.createAt(LocalDateTime.now().minusDays(1))
			.userId(2L)
			.nickName("test_user_2")
			.userProfileImage("user_profile_image_2")
			.rating(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
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
}
