package app.bottlenote.docs.review;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewQueryFixture {

	public ReviewModifyRequest getReviewModifyRequest() {
		return new ReviewModifyRequest("맛있습니다.", null, null, null, null, null);
	}

	public ReviewDetail getReview() {
		return getReviews().content().getReviewList().get(0);
	}

	public PageResponse<ReviewResponse> getReviews() {
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
			.reviewTastingTag(List.of("달콤한맛"))
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
			.reviewTastingTag(List.of("xxx맛"))
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

	public PageResponse<ReviewResponse> getReviewsByMe() {
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
			.reviewContent("이 가게에서 완전 가성비로 먹었어요")
			.price(BigDecimal.valueOf(50000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(3L)
			.replyCount(6L)
			.reviewImageUrl("https://picsum.photos/600/600")
			.createAt(LocalDateTime.now().minusDays(1))
			.userId(1L)
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
