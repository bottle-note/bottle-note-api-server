package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.alcohols.dto.response.CategoryResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailInfo;
import app.bottlenote.alcohols.dto.response.ReviewsDetailInfo;
import app.bottlenote.alcohols.facade.payload.FriendInfo;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.dto.vo.UserInfo;

import java.math.BigDecimal;
import java.util.List;

import static app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup.BLEND;
import static app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup.BOURBON;
import static app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup.OTHER;
import static app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup.RYE;
import static app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static app.bottlenote.review.domain.constant.SizeType.GLASS;

public class AlcoholQueryFixture {
	// 응답값반환값

	public AlcoholDetailInfo getAlcoholDetailInfo() {
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
			.abv("46")
			.korDistillery("글래스고 디스틸러리")
			.engDistillery("The Glasgow Distillery Co.")
			.rating(3.5)
			.totalRatingsCount(3L)
			.myRating(4.5)
			.myAvgRating(2.5)
			.isPicked(true)
			.alcoholsTastingTags("달달한,부드러운,향긋한,견과류,후추향의")
			.build();
	}

	public FriendsDetailInfo getFriendsDetailInfo() {
		return FriendsDetailInfo.of(
			6L,
			List.of(
				FriendInfo.of("https://picsum.photos/600/600", 1L, "늙은코끼리", 4.5),
				FriendInfo.of("https://picsum.photos/600/600", 2L, "나무사자", 1.5),
				FriendInfo.of("https://picsum.photos/600/600", 3L, "피자파인애플", 3.0),
				FriendInfo.of("https://picsum.photos/600/600", 4L, "멘토스", 0.5),
				FriendInfo.of("https://picsum.photos/600/600", 5L, "민트맛치토스", 5.0),
				FriendInfo.of("https://picsum.photos/600/600", 6L, "목데이터", 1.0)
			)
		);
	}

	public ReviewsDetailInfo getReviewsDetailInfo() {
		List<ReviewInfo> bestReview = List.of(
			ReviewInfo.builder()
				.userInfo(new UserInfo(1L, null, "3342네임"))
				.reviewId(3L).reviewContent("약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
				.rating(5.0).sizeType(GLASS).price(BigDecimal.valueOf(150000.00)).viewCount(0L).likeCount(0L).isLikedByMe(false)
				.replyCount(2L).hasReplyByMe(false).status(null).reviewImageUrl(null).createAt(null).build()
		);

		List<ReviewInfo> recentReviewInfos = List.of(
			ReviewInfo.builder()
				.userInfo(new UserInfo(1L, null, "xcvx"))
				.reviewId(3L).reviewContent("약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
				.rating(5.0).sizeType(GLASS).price(BigDecimal.valueOf(150000.00)).viewCount(0L).likeCount(0L).isLikedByMe(false)
				.replyCount(2L).hasReplyByMe(false).status(null).reviewImageUrl(null).createAt(null).build(),
			ReviewInfo.builder()
				.userInfo(new UserInfo(1L, null, "ghjnn"))
				.reviewId(5L).reviewContent("맛있어요")
				.rating(4.5).sizeType(null).price(BigDecimal.valueOf(0.00)).viewCount(0L).likeCount(0L).isLikedByMe(false)
				.replyCount(0L).hasReplyByMe(false).status(null).reviewImageUrl(null).createAt(null).build(),
			ReviewInfo.builder()
				.userInfo(new UserInfo(1L, null, "asaa"))
				.reviewId(7L).reviewContent("이 위스키는 스파이시한 오크와 달콤한 과일 노트가 절묘하게 어우러져 있어요. 피니시는 길고 부드러워요.")
				.rating(4.5).sizeType(null).price(BigDecimal.valueOf(0.00)).viewCount(0L).likeCount(0L).isLikedByMe(false)
				.replyCount(0L).hasReplyByMe(false).status(null).reviewImageUrl(null).createAt(null).build()
		);

		return ReviewsDetailInfo.builder()
			.totalReviewCount(10L)
			.bestReviewInfos(bestReview)
			.recentReviewInfos(recentReviewInfos)
			.build();
	}

	public AlcoholSearchRequest getRequest() {
		return AlcoholSearchRequest.builder()
			.keyword("glen")
			.category(SINGLE_MALT)
			.regionId(1L)
			.sortType(SearchSortType.REVIEW)
			.sortOrder(SortOrder.DESC)
			.cursor(0L)
			.pageSize(3L)
			.build();
	}

	public PageResponse<AlcoholSearchResponse> getResponse() {

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
			.isPicked(false)
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
			.isPicked(true)
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
			.isPicked(true)
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

	public List<CategoryResponse> categoryResponses() {
		return List.of(
			new CategoryResponse("SINGLE_MOLT", "싱글 몰트", SINGLE_MALT),
			new CategoryResponse("BLENDED", "블렌디드", BLEND),
			new CategoryResponse("BOURBON", "버번", BOURBON),
			new CategoryResponse("RYE", "라이", RYE),
			new CategoryResponse("OTHER", "기타", OTHER)
		);
	}
}
