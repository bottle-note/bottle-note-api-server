package app.bottlenote.docs.review;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.controller.ReviewController;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.request.ReviewStatusChangeRequest;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.DELETE_SUCCESS;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.PRIVATE_SUCCESS;
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

@DisplayName("[restdocs] 리뷰 컨트롤러 RestDocs용 테스트")
class RestReviewControllerDocsTest extends AbstractRestDocs {

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
						fieldWithPath("locationInfo.locationName").type(STRING).description("상호 명").optional(),
						fieldWithPath("locationInfo.zipCode").type(STRING).description("우편번호").optional(),
						fieldWithPath("locationInfo.address").type(STRING).description("주소").optional(),
						fieldWithPath("locationInfo.detailAddress").type(STRING).description("상세 주소").optional(),
						fieldWithPath("locationInfo.category").type(STRING).description("카테고리").optional(),
						fieldWithPath("locationInfo.mapUrl").type(STRING).description("지도 URL").optional(),
						fieldWithPath("locationInfo.latitude").type(STRING).description("위도(x좌표)").optional(),
						fieldWithPath("locationInfo.longitude").type(STRING).description("경도(y좌표)").optional(),
						fieldWithPath("imageUrlList").type(ARRAY).description("이미지 URL 목록"),
						fieldWithPath("imageUrlList[].order").type(NUMBER).description("이미지 순서"),
						fieldWithPath("imageUrlList[].viewUrl").type(STRING).description("이미지 뷰 URL"),
						fieldWithPath("tastingTagList[]").type(ARRAY).description("테이스팅 태그 목록"),
						fieldWithPath("rating").description("리뷰별점 (해당 리뷰를 남길 시점의 별점 )")

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
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간"),
						fieldWithPath("meta.serverPathVersion").type(STRING).description("서버 경로 버전")
					)
				)
			);
	}

	@Test
	@DisplayName("리뷰 목록을 조회할 수 있다.")
	void review_read_test() throws Exception {

		//given
		PageResponse<ReviewListResponse> response = ReviewObjectFixture.getReviewListResponse();

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
						fieldWithPath("data.totalCount").description("해당 술의 총 리뷰 수"),
						fieldWithPath("data.reviewList[].reviewId").description("리뷰 ID"),
						fieldWithPath("data.reviewList[].reviewContent").description("리뷰 내용"),
						fieldWithPath("data.reviewList[].price").description("리뷰 가격"),
						fieldWithPath("data.reviewList[].sizeType").optional().description("리뷰 사이즈 타입"),
						fieldWithPath("data.reviewList[].likeCount").description("리뷰 좋아요 수"),
						fieldWithPath("data.reviewList[].replyCount").description("리뷰 댓글 수"),
						fieldWithPath("data.reviewList[].reviewImageUrl").description("리뷰 이미지 URL"),
						fieldWithPath("data.reviewList[].totalImageCount").description("리뷰 이미지 총 개수"),
						fieldWithPath("data.reviewList[].userInfo.userId").description("리뷰 작성자 ID"),
						fieldWithPath("data.reviewList[].userInfo.nickName").description("리뷰 작성자 닉네임"),
						fieldWithPath("data.reviewList[].userInfo.userProfileImage").description("리뷰 작성자 프로필 이미지 URL"),
						fieldWithPath("data.reviewList[].rating").description("리뷰 평점"),
						fieldWithPath("data.reviewList[].viewCount").description("리뷰 조회수"),
						fieldWithPath("data.reviewList[].locationInfo").description("리뷰 장소 정보"),
						fieldWithPath("data.reviewList[].locationInfo.name").description("리뷰 장소 명"),
						fieldWithPath("data.reviewList[].locationInfo.zipCode").description("리뷰 장소 우편번호"),
						fieldWithPath("data.reviewList[].locationInfo.address").description("리뷰 장소 주소"),
						fieldWithPath("data.reviewList[].locationInfo.detailAddress").description("리뷰 장소 상세 주소"),
						fieldWithPath("data.reviewList[].locationInfo.category").description("리뷰 장소 카테고리"),
						fieldWithPath("data.reviewList[].locationInfo.mapUrl").description("리뷰 장소 지도 URL"),
						fieldWithPath("data.reviewList[].locationInfo.latitude").description("리뷰 장소 위도"),
						fieldWithPath("data.reviewList[].locationInfo.longitude").description("리뷰 장소 경도"),
						fieldWithPath("data.reviewList[].status").description("리뷰 공개 여부 (PUBLIC/PRIVATE)"),
						fieldWithPath("data.reviewList[].isMyReview").description("내가 작성한 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].isLikedByMe").description("내가 좋아요를 눌렀는지 여부"),
						fieldWithPath("data.reviewList[].hasReplyByMe").description("내가 댓글을 달았는지 여부"),
						fieldWithPath("data.reviewList[].isBestReview").description("베스트 리뷰 여부"),
						fieldWithPath("data.reviewList[].tastingTagList").description("리뷰 테이스팅 태그 목록"),
						fieldWithPath("data.reviewList[].createAt").description("리뷰 작성 날짜 'yyyyMMddHHmm' 포맷"),
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
		PageResponse<ReviewListResponse> response = ReviewObjectFixture.getReviewListResponse();

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.getMyReviews(any(), any(), any())).thenReturn(response);

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
						fieldWithPath("data.totalCount").description("해당 술의 총 리뷰 수"),
						fieldWithPath("data.reviewList[].reviewId").description("리뷰 ID"),
						fieldWithPath("data.reviewList[].reviewContent").description("리뷰 내용"),
						fieldWithPath("data.reviewList[].price").description("리뷰 가격"),
						fieldWithPath("data.reviewList[].sizeType").optional().description("리뷰 사이즈 타입"),
						fieldWithPath("data.reviewList[].likeCount").description("리뷰 좋아요 수"),
						fieldWithPath("data.reviewList[].replyCount").description("리뷰 댓글 수"),
						fieldWithPath("data.reviewList[].reviewImageUrl").description("리뷰 이미지 URL"),
						fieldWithPath("data.reviewList[].totalImageCount").description("리뷰 이미지 총 개수"),
						fieldWithPath("data.reviewList[].userInfo.userId").description("리뷰 작성자 ID"),
						fieldWithPath("data.reviewList[].userInfo.nickName").description("리뷰 작성자 닉네임"),
						fieldWithPath("data.reviewList[].userInfo.userProfileImage").description("리뷰 작성자 프로필 이미지 URL"),
						fieldWithPath("data.reviewList[].rating").description("리뷰 평점"),
						fieldWithPath("data.reviewList[].viewCount").description("리뷰 조회수"),
						fieldWithPath("data.reviewList[].locationInfo").description("리뷰 장소 정보"),
						fieldWithPath("data.reviewList[].locationInfo.name").description("리뷰 장소 명"),
						fieldWithPath("data.reviewList[].locationInfo.zipCode").description("리뷰 장소 우편번호"),
						fieldWithPath("data.reviewList[].locationInfo.address").description("리뷰 장소 주소"),
						fieldWithPath("data.reviewList[].locationInfo.detailAddress").description("리뷰 장소 상세 주소"),
						fieldWithPath("data.reviewList[].locationInfo.category").description("리뷰 장소 카테고리"),
						fieldWithPath("data.reviewList[].locationInfo.mapUrl").description("리뷰 장소 지도 URL"),
						fieldWithPath("data.reviewList[].locationInfo.latitude").description("리뷰 장소 위도"),
						fieldWithPath("data.reviewList[].locationInfo.longitude").description("리뷰 장소 경도"),
						fieldWithPath("data.reviewList[].status").description("리뷰 공개 여부 (PUBLIC/PRIVATE)"),
						fieldWithPath("data.reviewList[].isMyReview").description("내가 작성한 리뷰인지 여부"),
						fieldWithPath("data.reviewList[].isLikedByMe").description("내가 좋아요를 눌렀는지 여부"),
						fieldWithPath("data.reviewList[].hasReplyByMe").description("내가 댓글을 달았는지 여부"),
						fieldWithPath("data.reviewList[].isBestReview").description("베스트 리뷰 여부"),
						fieldWithPath("data.reviewList[].tastingTagList").description("리뷰 테이스팅 태그 목록"),
						fieldWithPath("data.reviewList[].createAt").description("리뷰 작성 날짜 'yyyyMMddHHmm' 포맷"),
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
	@DisplayName("리뷰를 상세조회할 수 있다.")
	void review_detail_read_test() throws Exception {

		ReviewDetailResponse reviewDetailResponse = ReviewObjectFixture.getReviewDetailResponse();

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.getDetailReview(anyLong(), anyLong())).thenReturn(reviewDetailResponse);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reviews/detail/1")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-detail-read",
					responseFields(
						fieldWithPath("success").description("요청 성공 여부").type(JsonFieldType.BOOLEAN),
						fieldWithPath("code").description("응답 코드").type(JsonFieldType.NUMBER),

						// Alcohol Info
						fieldWithPath("data.alcoholInfo.alcoholId").description("술 ID").type(JsonFieldType.NUMBER),
						fieldWithPath("data.alcoholInfo.korName").description("술의 한국어 이름").type(JsonFieldType.STRING),
						fieldWithPath("data.alcoholInfo.engName").description("술의 영어 이름").type(JsonFieldType.STRING),
						fieldWithPath("data.alcoholInfo.korCategoryName").description("술 카테고리의 한국어 이름").type(JsonFieldType.STRING),
						fieldWithPath("data.alcoholInfo.engCategoryName").description("술 카테고리의 영어 이름").type(JsonFieldType.STRING),
						fieldWithPath("data.alcoholInfo.imageUrl").description("술 이미지 URL").type(JsonFieldType.STRING),
						fieldWithPath("data.alcoholInfo.isPicked").description("선택 여부").type(JsonFieldType.BOOLEAN),

						// Review Response
						fieldWithPath("data.reviewInfo.reviewId").description("리뷰 ID").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.reviewContent").description("리뷰 내용").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.price").description("가격").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.sizeType").description("사이즈 타입").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.likeCount").description("좋아요 개수").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.replyCount").description("댓글 개수").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.reviewImageUrl").description("리뷰 이미지 URL").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.totalImageCount").description("리뷰 이미지 총 개수"),

						// User Info
						fieldWithPath("data.reviewInfo.userInfo").description("사용자 정보").type(JsonFieldType.OBJECT),
						fieldWithPath("data.reviewInfo.userInfo.userId").description("사용자 ID").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.userInfo.nickName").description("닉네임").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.userInfo.userProfileImage").description("사용자 프로필 이미지 URL").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.rating").description("평점").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewInfo.viewCount").description("조회수"),

						// Location Info
						fieldWithPath("data.reviewInfo.locationInfo").description("위치 정보").type(JsonFieldType.OBJECT).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.name").description("상호 명").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.zipCode").description("우편번호").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.address").description("주소").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.detailAddress").description("상세주소").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.category").description("카테고리").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.mapUrl").description("지도 URL").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.latitude").description("위도").type(JsonFieldType.STRING).optional(),
						fieldWithPath("data.reviewInfo.locationInfo.longitude").description("경도").type(JsonFieldType.STRING).optional(),

						fieldWithPath("data.reviewInfo.status").description("리뷰 상태").type(JsonFieldType.STRING),
						fieldWithPath("data.reviewInfo.isMyReview").description("내 리뷰 여부").type(JsonFieldType.BOOLEAN),
						fieldWithPath("data.reviewInfo.isLikedByMe").description("내가 좋아요를 눌렀는지 여부").type(JsonFieldType.BOOLEAN),
						fieldWithPath("data.reviewInfo.hasReplyByMe").description("내가 댓글을 달았는지 여부").type(JsonFieldType.BOOLEAN),
						fieldWithPath("data.reviewInfo.isBestReview").description("베스트 리뷰 여부").type(JsonFieldType.BOOLEAN),
						fieldWithPath("data.reviewInfo.tastingTagList").description("리뷰 테이스팅 태그 목록"),
						fieldWithPath("data.reviewInfo.createAt").description("리뷰 작성 시간"),

						// Review Image List
						fieldWithPath("data.reviewImageList").description("리뷰 이미지 목록").type(JsonFieldType.ARRAY),
						fieldWithPath("data.reviewImageList[].order").description("이미지 순서").type(JsonFieldType.NUMBER),
						fieldWithPath("data.reviewImageList[].viewUrl").description("이미지 URL").type(JsonFieldType.STRING),

						fieldWithPath("errors").description("에러 목록").type(JsonFieldType.ARRAY),

						// Meta Information
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored()
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

		when(reviewService.modifyReview(any(ReviewModifyRequest.class), any(), any())).thenReturn(ReviewResultResponse.response(MODIFY_SUCCESS, reviewId));

		//then
		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/reviews/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PUBLIC)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-update",
					requestFields(
						fieldWithPath("content").type(STRING).description("리뷰 내용").optional(),
						fieldWithPath("status").type(STRING).description("리뷰 상태").optional(),
						fieldWithPath("price").type(NUMBER).description("가격").optional(),
						fieldWithPath("sizeType").type(STRING).description("술 타입 (잔 or 병)").optional(),
						fieldWithPath("locationInfo").type(OBJECT).description("위치 정보").optional(),
						fieldWithPath("locationInfo.locationName").type(STRING).description("상호 명").optional(),
						fieldWithPath("locationInfo.zipCode").type(STRING).description("우편번호").optional(),
						fieldWithPath("locationInfo.address").type(STRING).description("주소").optional(),
						fieldWithPath("locationInfo.detailAddress").type(STRING).description("상세 주소").optional(),
						fieldWithPath("locationInfo.category").type(STRING).description("카테고리").optional(),
						fieldWithPath("locationInfo.mapUrl").type(STRING).description("지도 URL").optional(),
						fieldWithPath("locationInfo.latitude").type(STRING).description("위도(x좌표)").optional(),
						fieldWithPath("locationInfo.longitude").type(STRING).description("경도(y좌표)").optional(),
						fieldWithPath("imageUrlList").type(ARRAY).description("이미지 URL 목록"),
						fieldWithPath("imageUrlList[].order").type(NUMBER).description("이미지 순서"),
						fieldWithPath("imageUrlList[].viewUrl").type(STRING).description("이미지 뷰 URL"),
						fieldWithPath("tastingTagList").description("테이스팅 태그 목록").optional()
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data").description("응답 데이터"),
						fieldWithPath("data.codeMessage").description("응답 코드 메시지"),
						fieldWithPath("data.message").description("응답 메시지"),
						fieldWithPath("data.reviewId").description("수정된 리뷰의 ID"),
						fieldWithPath("data.responseAt").description("응답 시간"),
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

	@Test
	@DisplayName("리뷰의 상태를 변경할 수 있다.")
	void review_status_chagne() throws Exception {

		Long reviewId = 1L;

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(reviewService.changeStatus(anyLong(), any(ReviewStatusChangeRequest.class), anyLong()))
			.thenReturn(ReviewResultResponse.response(PRIVATE_SUCCESS, reviewId));

		//then
		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/reviews/{reviewId}/display", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ReviewStatusChangeRequest(ReviewDisplayStatus.PRIVATE)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/review-status-change",
					requestFields(
						fieldWithPath("status").type(STRING).description("리뷰 상태")
					),
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
