package app.bottlenote.docs.alcohols;

import app.bottlenote.alcohols.controller.AlcoholQueryController;
import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.detail.FriendsDetailInfo;
import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static app.bottlenote.review.domain.constant.SizeType.GLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("alcohol 컨트롤러 RestDocs용 테스트")
class RestAlcoholQueryControllerTest extends AbstractRestDocs {

	private final AlcoholQueryService alcoholQueryService = mock(AlcoholQueryService.class);

	@Override
	protected Object initController() {
		return new AlcoholQueryController(alcoholQueryService);
	}

	@DisplayName("술 리스트를 조회할 수 있다.")
	@Test
	void docs_1() throws Exception {
		// given
		Long userId = 1L;
		AlcoholSearchRequest request = getRequest();
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(any(AlcoholSearchRequest.class), any())).thenReturn(response);

		// then
		mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", "glen")
				.param("category", "SINGLE_MOLT")
				.param("regionId", "1")
				.param("sortType", "REVIEW")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "3")
			)
			.andExpect(status().isOk())
			.andDo(
				document("alcohols/search",
					queryParameters(
						parameterWithName("keyword").optional().description("검색어"),
						parameterWithName("category").optional().description("카테고리 (category API 참조)"),
						parameterWithName("regionId").optional().description("지역 ID (region API 참조)"),
						parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
						parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
						parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
						parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.totalCount").description("전체 술 리스트의 크기"),
						fieldWithPath("data.alcohols[].alcoholId").description("술 ID"),
						fieldWithPath("data.alcohols[].korName").description("술 한글 이름"),
						fieldWithPath("data.alcohols[].engName").description("술 영문 이름"),
						fieldWithPath("data.alcohols[].korCategoryName").description("술 한글 카테고리 이름"),
						fieldWithPath("data.alcohols[].engCategoryName").description("술 영문 카테고리 이름"),
						fieldWithPath("data.alcohols[].imageUrl").description("술 이미지 URL"),
						fieldWithPath("data.alcohols[].rating").description("술 평점"),
						fieldWithPath("data.alcohols[].ratingCount").description("술 평점 개수"),
						fieldWithPath("data.alcohols[].reviewCount").description("술 리뷰 개수"),
						fieldWithPath("data.alcohols[].pickCount").description("술 찜 개수"),
						fieldWithPath("data.alcohols[].picked").description("술 찜 여부"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.pageable").description("페이징 정보"),
						fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
						fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
						fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
						fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"),
						fieldWithPath("meta.searchParameters.keyword").description("검색 시 사용 한 검색어"),
						fieldWithPath("meta.searchParameters.category").description("검색 시 사용 한 카테고리"),
						fieldWithPath("meta.searchParameters.regionId").description("검색 시 사용 한 지역 ID"),
						fieldWithPath("meta.searchParameters.sortType").description("검색 시 사용 한 정렬 타입"),
						fieldWithPath("meta.searchParameters.sortOrder").description("검색 시 사용 한 정렬 순서"),
						fieldWithPath("meta.searchParameters.cursor").description("검색 시 사용 한 커서"),
						fieldWithPath("meta.searchParameters.pageSize").description("검색 시 사용 한 페이지 사이즈")

					)
				)
			);

	}

	@DisplayName("술의 상세 정보를 조회 할 수 있다.")
	@Test
	void docs_2() throws Exception {
		AlcoholDetail detail = AlcoholDetail.of(
			getAlcoholDetailInfo(),
			getFriendsDetailInfo(),
			getReviewsDetailInfo()
		);

		when(alcoholQueryService.findAlcoholDetailById(any(), any())).thenReturn(detail);

		mockMvc.perform(get("/api/v1/alcohols/{alcoholId}", 1))
			.andExpect(status().isOk())
			.andDo(print())
			.andDo(
				document("alcohols/detail",
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),

						fieldWithPath("data.alcohols.alcoholId").description("술 ID"),
						fieldWithPath("data.alcohols.alcoholUrlImg").description("술 이미지 URL"),
						fieldWithPath("data.alcohols.korName").description("술의 한국어 이름"),
						fieldWithPath("data.alcohols.engName").description("술의 영어 이름"),
						fieldWithPath("data.alcohols.korCategory").description("술의 한국어 카테고리"),
						fieldWithPath("data.alcohols.engCategory").description("술의 영어 카테고리"),
						fieldWithPath("data.alcohols.korRegion").description("술의 한국어 지역"),
						fieldWithPath("data.alcohols.engRegion").description("술의 영어 지역"),
						fieldWithPath("data.alcohols.cask").description("술의 숙성 캐스크 정보"),
						fieldWithPath("data.alcohols.avg").description("술의 도수"),
						fieldWithPath("data.alcohols.korDistillery").description("술 제조사의 한국어 이름"),
						fieldWithPath("data.alcohols.engDistillery").description("술 제조사의 영어 이름"),
						fieldWithPath("data.alcohols.rating").description("술의 평균 평점"),
						fieldWithPath("data.alcohols.totalRatingsCount").description("총 평점 참여자 수"),
						fieldWithPath("data.alcohols.myRating").description("내가 준 평점"),
						fieldWithPath("data.alcohols.isPicked").description("내가 한 좋아요 술인지 여부"),
						fieldWithPath("data.alcohols.tags").description("술의 태그 목록"),

						fieldWithPath("data.friendsInfo.followerCount").description("팔로워 수"),
						fieldWithPath("data.friendsInfo.friends[].user_image_url").description("친구의 프로필 이미지 URL"),
						fieldWithPath("data.friendsInfo.friends[].userId").description("친구의 사용자 ID"),
						fieldWithPath("data.friendsInfo.friends[].nickName").description("친구의 닉네임"),
						fieldWithPath("data.friendsInfo.friends[].rating").description("친구의 평점"),

						fieldWithPath("data.reviews.totalReviewCount").description("해당 술의 총 리뷰 수"),
						fieldWithPath("data.reviews.bestReviewInfos[].userId").description("베스트 리뷰 작성자 ID"),
						fieldWithPath("data.reviews.bestReviewInfos[].imageUrl").description("베스트 리뷰 작성자 프로필 이미지 URL"),
						fieldWithPath("data.reviews.bestReviewInfos[].nickName").description("베스트 리뷰 작성자 닉네임"),
						fieldWithPath("data.reviews.bestReviewInfos[].reviewId").description("베스트 리뷰 ID"),
						fieldWithPath("data.reviews.bestReviewInfos[].reviewContent").description("베스트 리뷰 내용"),
						fieldWithPath("data.reviews.bestReviewInfos[].rating").description("베스트 리뷰 평점"),
						fieldWithPath("data.reviews.bestReviewInfos[].sizeType").optional().description("베스트 리뷰 사이즈 타입"),
						fieldWithPath("data.reviews.bestReviewInfos[].price").description("베스트 리뷰 가격"),
						fieldWithPath("data.reviews.bestReviewInfos[].viewCount").description("베스트 리뷰 조회수"),
						fieldWithPath("data.reviews.bestReviewInfos[].likeCount").description("베스트 리뷰 좋아요 수"),
						fieldWithPath("data.reviews.bestReviewInfos[].isMyLike").description("베스트 리뷰 내가 좋아요를 눌렀는지 여부"),
						fieldWithPath("data.reviews.bestReviewInfos[].replyCount").description("베스트 리뷰 댓글 수"),
						fieldWithPath("data.reviews.bestReviewInfos[].isMyReply").description("베스트 리뷰 내가 댓글을 달았는지 여부"),
						fieldWithPath("data.reviews.bestReviewInfos[].status").description("리뷰 공개 비공개 여부 (PUBLIC/PRIVATE)"),
						fieldWithPath("data.reviews.bestReviewInfos[].reviewImageUrl").description("베스트 리뷰 이미지 URL"),
						fieldWithPath("data.reviews.bestReviewInfos[].createAt").description("베스트 리뷰 작성 날짜"),

						fieldWithPath("data.reviews.recentReviewInfos[].userId").description("최신 리뷰 작성자 ID"),
						fieldWithPath("data.reviews.recentReviewInfos[].imageUrl").description("최신 리뷰 작성자 프로필 이미지 URL"),
						fieldWithPath("data.reviews.recentReviewInfos[].nickName").description("최신 리뷰 작성자 닉네임"),
						fieldWithPath("data.reviews.recentReviewInfos[].reviewId").description("최신 리뷰 ID"),
						fieldWithPath("data.reviews.recentReviewInfos[].reviewContent").description("최신 리뷰 내용"),
						fieldWithPath("data.reviews.recentReviewInfos[].rating").description("최신 리뷰 평점"),
						fieldWithPath("data.reviews.recentReviewInfos[].sizeType").optional().description("최신 리뷰 사이즈 타입"),
						fieldWithPath("data.reviews.recentReviewInfos[].price").description("최신 리뷰 가격"),
						fieldWithPath("data.reviews.recentReviewInfos[].viewCount").description("최신 리뷰 조회수"),
						fieldWithPath("data.reviews.recentReviewInfos[].likeCount").description("최신 리뷰 좋아요 수"),
						fieldWithPath("data.reviews.recentReviewInfos[].isMyLike").description("최신 리뷰 내가 좋아요를 눌렀는지 여부"),
						fieldWithPath("data.reviews.recentReviewInfos[].replyCount").description("최신 리뷰 댓글 수"),
						fieldWithPath("data.reviews.recentReviewInfos[].isMyReply").description("최신 리뷰 내가 댓글을 달았는지 여부"),
						fieldWithPath("data.reviews.recentReviewInfos[].status").description("리뷰 공개 비공개 여부 (PUBLIC/PRIVATE)"),
						fieldWithPath("data.reviews.recentReviewInfos[].reviewImageUrl").description("최신 리뷰 이미지 URL"),
						fieldWithPath("data.reviews.recentReviewInfos[].createAt").description("최신 리뷰 작성 날짜"),

						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored()
					)
				)
			);
	}

	private AlcoholDetailInfo getAlcoholDetailInfo() {
		return AlcoholDetailInfo.builder()
			.alcoholId(1L)
			.alcoholUrlImg("https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg")
			.korName("글래스고 1770 싱글몰트 스카치 위스키")
			.engName("1770 Glasgow Single Malt")
			.korCategory("싱글 몰트")
			.engCategory("Single Malt")
			.korRegion("스코틀랜드/하이랜드")
			.engRegion("Scotland/Highlands")
			.cask("Marriage of Ex-Bourbon & Virgin Oak Casks")
			.avg("46")
			.korDistillery("글래스고 디스틸러리")
			.engDistillery("The Glasgow Distillery Co.")
			.rating(3.5)
			.totalRatingsCount(3L)
			.myRating(4.5)
			.isPicked(true)
			.tags(List.of("달달한", "부드러운", "향긋한", "견과류", "후추향의"))
			.build();
	}

	private FriendsDetailInfo getFriendsDetailInfo() {
		return FriendsDetailInfo.of(
			6L,
			List.of(
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 1L, "늙은코끼리", 4.5),
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 2L, "나무사자", 1.5),
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 3L, "피자파인애플", 3.0),
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 4L, "멘토스", 0.5),
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 5L, "민트맛치토스", 5.0),
				FriendsDetailInfo.FriendInfo.of("https://picsum.photos/600/600", 6L, "목데이터", 1.0)
			)
		);
	}

	private ReviewsDetailInfo getReviewsDetailInfo() {
		List<ReviewsDetailInfo.ReviewInfo> bestReview = List.of(
			ReviewsDetailInfo.ReviewInfo.builder()
				.userId(1L).imageUrl(null).nickName("3342네임")
				.reviewId(3L).reviewContent("약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
				.rating(5.0).sizeType(GLASS).price(BigDecimal.valueOf(150000.00)).viewCount(0L).likeCount(0L).isMyLike(false)
				.replyCount(2L).isMyReply(false).status(null).reviewImageUrl(null).createAt(null).build()
		);

		List<ReviewsDetailInfo.ReviewInfo> recentReviewInfos = List.of(
			ReviewsDetailInfo.ReviewInfo.builder()
				.userId(1L).imageUrl(null).nickName("wnrdms123")
				.reviewId(3L).reviewContent("약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
				.rating(5.0).sizeType(GLASS).price(BigDecimal.valueOf(150000.00)).viewCount(0L).likeCount(0L).isMyLike(false)
				.replyCount(2L).isMyReply(false).status(null).reviewImageUrl(null).createAt(null).build(),
			ReviewsDetailInfo.ReviewInfo.builder()
				.userId(2L).imageUrl(null).nickName("3213dsadsa")
				.reviewId(5L).reviewContent("맛있어요")
				.rating(4.5).sizeType(null).price(BigDecimal.valueOf(0.00)).viewCount(0L).likeCount(0L).isMyLike(false)
				.replyCount(0L).isMyReply(false).status(null).reviewImageUrl(null).createAt(null).build(),
			ReviewsDetailInfo.ReviewInfo.builder()
				.userId(2L).imageUrl(null).nickName("죽은 공룡")
				.reviewId(7L).reviewContent("이 위스키는 스파이시한 오크와 달콤한 과일 노트가 절묘하게 어우러져 있어요. 피니시는 길고 부드러워요.")
				.rating(4.5).sizeType(null).price(BigDecimal.valueOf(0.00)).viewCount(0L).likeCount(0L).isMyLike(false)
				.replyCount(0L).isMyReply(false).status(null).reviewImageUrl(null).createAt(null).build()
		);

		return ReviewsDetailInfo.builder()
			.totalReviewCount(10L)
			.bestReviewInfos(bestReview)
			.recentReviewInfos(recentReviewInfos)
			.build();
	}

	private AlcoholSearchRequest getRequest() {
		return AlcoholSearchRequest.builder()
			.keyword("glen")
			.category("SINGLE_MOLT")
			.regionId(1L)
			.sortType(SearchSortType.REVIEW)
			.sortOrder(SortOrder.DESC)
			.cursor(0L)
			.pageSize(3L)
			.build();
	}

	private PageResponse<AlcoholSearchResponse> getResponse() {

		AlcoholsSearchDetail detail_1 = AlcoholsSearchDetail.builder()
			.alcoholId(5L)
			.korName("아녹 24년")
			.engName("anCnoc 24-year-old")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/6/6/989/270671-big.jpg")
			.rating(4.5)
			.ratingCount(1L)
			.reviewCount(0L)
			.pickCount(1L)
			.picked(false)
			.build();

		AlcoholsSearchDetail detail_2 = AlcoholsSearchDetail.builder()
			.alcoholId(1L)
			.korName("글래스고 1770 싱글몰트 스카치 위스키")
			.engName("1770 Glasgow Single Malt")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg")
			.rating(3.5)
			.ratingCount(3L)
			.reviewCount(1L)
			.pickCount(1L)
			.picked(true)
			.build();

		AlcoholsSearchDetail detail_3 = AlcoholsSearchDetail.builder()
			.alcoholId(2L)
			.korName("글래스고 1770 싱글몰트 스카치 위스키")
			.engName("1770 Glasgow Single Malt")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8888/404535-big.jpg")
			.rating(3.5)
			.ratingCount(1L)
			.reviewCount(0L)
			.pickCount(1L)
			.picked(true)
			.build();


		Long totalCount = 5L;
		List<AlcoholsSearchDetail> details = List.of(detail_1, detail_2, detail_3);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(4L)
			.pageSize(3L)
			.hasNext(true)
			.build();
		AlcoholSearchResponse response = AlcoholSearchResponse.of(totalCount, details);
		return PageResponse.of(response, cursorPageable);
	}

}
