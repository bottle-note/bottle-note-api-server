package app.bottlenote.review.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewObjectFixture {

	private static final String content = "맛있어요";

	public static ReviewCreateRequest getReviewCreateRequest() {
		return new ReviewCreateRequest(
			1L,
			ReviewDisplayStatus.PUBLIC,
			content,
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"),
			List.of(
				new ReviewImageInfo(1L, "url1"),
				new ReviewImageInfo(2L, "url2"),
				new ReviewImageInfo(3L, "url3")
			),
			List.of("테이스팅태그 1", "테이스팅태그 2", "테이스팅태그 3")
		);
	}

	public static ReviewCreateResponse getReviewCreateResponse() {
		return ReviewCreateResponse.builder()
			.id(1L)
			.content(content)
			.callback(String.valueOf(1L))
			.build();
	}

	public static Review getReviewFixture() {
		Long alcoholId = 1L;
		Long userId = 1L;

		return Review.builder()
			.id(1L)
			.alcoholId(alcoholId)
			.userId(userId)
			.content(content)
			.build();
	}

	public static PageableRequest getEmptyPageableRequest() {
		return PageableRequest.builder().build();
	}

	public static PageResponse<ReviewResponse> getReviewListResponse() {

		ReviewDetail reviewDetail_1 = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent(content)
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
			.status(ReviewDisplayStatus.PUBLIC)
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
			.status(ReviewDisplayStatus.PUBLIC)
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

	public static ReviewModifyRequest getReviewModifyRequest() {
		return new ReviewModifyRequest(
			"그저 그래요",
			ReviewDisplayStatus.PUBLIC,
			BigDecimal.valueOf(10000L),
			List.of(new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")),
			SizeType.GLASS,
			List.of(),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"));
	}

	public static Alcohol getAlcoholFixture() {
		return Alcohol.builder()
			.id(1L)
			.build();
	}

	public static User getUserFixture() {
		return User.builder()
			.id(1L)
			.build();
	}

}
