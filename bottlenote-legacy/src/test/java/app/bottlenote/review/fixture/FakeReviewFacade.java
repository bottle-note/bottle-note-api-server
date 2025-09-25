package app.bottlenote.review.fixture;

import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.shared.review.constant.ReviewDisplayStatus;
import app.bottlenote.shared.review.constant.SizeType;
import app.bottlenote.shared.review.dto.response.ReviewListResponse;
import app.bottlenote.shared.review.payload.ReviewInfo;
import app.bottlenote.shared.review.payload.UserInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeReviewFacade implements ReviewFacade {
  private static final Logger log = LogManager.getLogger(FakeReviewFacade.class);
  public final Map<Long, ReviewInfo> reviewDatabase = new HashMap<>();

  // 생성자에서 초기 데이터 추가
  public FakeReviewFacade() {
    // 예시 데이터 추가
    reviewDatabase.put(
        1L,
        ReviewInfo.builder()
            .reviewId(1L)
            .reviewContent("정말 맛있는 제품이에요!")
            .reviewImageUrl("https://example.com/image1.jpg")
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

    reviewDatabase.put(
        2L,
        ReviewInfo.builder()
            .reviewId(2L)
            .reviewContent("괜찮은 제품이지만 별로에요.")
            .reviewImageUrl("https://example.com/image2.jpg")
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

    reviewDatabase.put(
        3L,
        ReviewInfo.builder()
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
    List<ReviewInfo> filteredReviews =
        reviewDatabase.values().stream()
            .filter(
                review ->
                    (alcoholId == null
                            || review
                                .sizeType()
                                .equals(SizeType.valueOf(alcoholId.toString()))) // 예시 필터링 조건
                        && (userId == null || review.userInfo().userId().equals(userId)))
            .toList();

    return ReviewListResponse.of((long) filteredReviews.size(), filteredReviews);
  }

  @Override
  public boolean isExistReview(Long reviewId) {
    return reviewId != null && reviewDatabase.containsKey(reviewId);
  }

  @Override
  public void requestBlockReview(Long reviewId) {
    if (isExistReview(reviewId)) {
      log.info("리뷰 ID: {}가 블록되었습니다.", reviewId);
      reviewDatabase.remove(reviewId);
    } else {
      log.info("리뷰 ID: {}는 존재하지 않습니다.", reviewId);
    }
  }

  @Override
  public Long getAlcoholIdByReviewId(Long reviewId) {
    return -1L;
  }

  public void addReview(ReviewInfo reviewInfo) {
    reviewDatabase.put(reviewInfo.reviewId(), reviewInfo);
  }

  @Override
  public ReviewInfo getReviewInfo(Long reviewId, Long userId) {
    return reviewDatabase.values().stream()
        .filter(
            review ->
                review.reviewId().equals(reviewId)
                    && (userId == null || review.userInfo().userId().equals(userId)))
        .findFirst()
        .orElse(null);
  }

  public void removeReview(Long reviewId) {
    reviewDatabase.remove(reviewId);
  }

  public ReviewInfo getReview(Long reviewId) {
    return reviewDatabase.get(reviewId);
  }
}
