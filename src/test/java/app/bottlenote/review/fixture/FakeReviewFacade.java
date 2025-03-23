package app.bottlenote.review.fixture;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import app.bottlenote.review.service.ReviewFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeReviewFacade implements ReviewFacade {

	private static final Logger log = LogManager.getLogger(FakeReviewFacade.class);

	private final Map<Long, ReviewInfo> fakeDatabase = new HashMap<>();

	// 생성자에서 초기 데이터 추가
	public FakeReviewFacade() {
		// 예시 데이터 추가
		fakeDatabase.put(1L, ReviewInfo.builder()
			.reviewId(1L)
			.reviewContent("정말 맛있는 제품이에요!")
			.reviewImageUrl("http://example.com/image1.jpg")
			.createAt(LocalDateTime.now().minusDays(2))
			.totalImageCount(3L)
			.userInfo(new UserInfo(1L, "nickName", "image"))
			.isMyReview(false)
			.status(ReviewDisplayStatus.PUBLIC)
			.isBestReview(true)
			.sizeType(SizeType.GLASS)
			.price(new BigDecimal("29.99"))
			.rating(4.5)
			.likeCount(10L)
			.replyCount(2L)
			.isLikedByMe(false)
			.hasReplyByMe(false)
			.viewCount(150L)
			.tastingTagList("fruity,spicy")
			.build());

		fakeDatabase.put(2L, ReviewInfo.builder()
			.reviewId(2L)
			.reviewContent("괜찮은 제품이지만 별로에요.")
			.reviewImageUrl("http://example.com/image2.jpg")
			.createAt(LocalDateTime.now().minusDays(5))
			.totalImageCount(1L)
			.userInfo(new UserInfo(1L, "nickName", "image"))
			.isMyReview(true)
			.status(ReviewDisplayStatus.PUBLIC)
			.isBestReview(false)
			.sizeType(SizeType.GLASS)
			.price(new BigDecimal("19.99"))
			.rating(3.0)
			.likeCount(5L)
			.replyCount(1L)
			.isLikedByMe(true)
			.hasReplyByMe(true)
			.viewCount(80L)
			.tastingTagList("sweet,creamy")
			.build());

		fakeDatabase.put(3L, ReviewInfo.builder()
			.reviewId(3L)
			.reviewContent("최고의 제품입니다! 다시 구매할게요.")
			.createAt(LocalDateTime.now().minusDays(1))
			.userInfo(new UserInfo(1L, "nickName", "image"))
			.isMyReview(false)
			.status(ReviewDisplayStatus.PUBLIC)
			.isBestReview(false)
			.sizeType(SizeType.GLASS)
			.price(new BigDecimal("39.99"))
			.rating(5.0)
			.likeCount(20L)
			.replyCount(5L)
			.isLikedByMe(false)
			.hasReplyByMe(false)
			.viewCount(200L)
			.tastingTagList("bitter,earthy")
			.build());
	}

	@Override
	public ReviewListResponse getReviewInfoList(Long alcoholId, Long userId) {
		List<ReviewInfo> filteredReviews = fakeDatabase.values().stream()
			.filter(review -> (alcoholId == null || review.sizeType().equals(SizeType.valueOf(alcoholId.toString()))) // 예시 필터링 조건
				&& (userId == null || review.userInfo().userId().equals(userId))).toList();

		return ReviewListResponse.of((long) filteredReviews.size(), filteredReviews);
	}

	@Override
	public boolean isExistReview(Long reviewId) {
		return reviewId != null && fakeDatabase.containsKey(reviewId);
	}

	@Override
	public void requestBlockReview(Long reviewId) {
		if (isExistReview(reviewId)) {
			log.info("리뷰 ID: {}가 블록되었습니다.", reviewId);
			fakeDatabase.remove(reviewId);
		} else {
			log.info("리뷰 ID: {}는 존재하지 않습니다.", reviewId);
		}
	}

	public void addReview(ReviewInfo reviewInfo) {
		fakeDatabase.put(reviewInfo.reviewId(), reviewInfo);
	}

	public void removeReview(Long reviewId) {
		fakeDatabase.remove(reviewId);
	}

	public ReviewInfo getReview(Long reviewId) {
		return fakeDatabase.get(reviewId);
	}

	@Override
	public Long getAlcoholIdByReviewId(Long reviewId) {
		return 0L;
	}
}
