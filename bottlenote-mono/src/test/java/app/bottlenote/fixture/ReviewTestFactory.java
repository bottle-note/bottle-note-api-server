package app.bottlenote.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.image.ImageInfo;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.ReviewReplyStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.user.domain.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ReviewTestFactory {

  private final Random random = new SecureRandom();

  @Autowired private EntityManager em;

  // ========== Review 생성 메서드들 ==========

  /** 기본 Review 생성 (User, Alcohol 객체 사용) */
  @Transactional
  @NotNull
  public Review persistReview(@NotNull User user, @NotNull Alcohol alcohol) {
    Review review =
        Review.builder()
            .userId(user.getId())
            .alcoholId(alcohol.getId())
            .content("훌륭한 위스키입니다. 부드럽고 깊은 맛이 일품이에요. " + generateRandomSuffix())
            .sizeType(SizeType.BOTTLE)
            .price(BigDecimal.valueOf(50000))
            .status(ReviewDisplayStatus.PUBLIC)
            .activeStatus(ReviewActiveStatus.ACTIVE)
            .build();
    em.persist(review);
    em.flush();
    return review;
  }

  /** ID로 Review 생성 */
  @Transactional
  @NotNull
  public Review persistReview(@NotNull Long userId, @NotNull Long alcoholId) {
    Review review =
        Review.builder()
            .userId(userId)
            .alcoholId(alcoholId)
            .content("훌륭한 위스키입니다. 부드럽고 깊은 맛이 일품이에요. " + generateRandomSuffix())
            .sizeType(SizeType.BOTTLE)
            .price(BigDecimal.valueOf(50000))
            .status(ReviewDisplayStatus.PUBLIC)
            .activeStatus(ReviewActiveStatus.ACTIVE)
            .build();
    em.persist(review);
    em.flush();
    return review;
  }

  /** 커스텀 내용으로 Review 생성 */
  @Transactional
  @NotNull
  public Review persistReview(
      @NotNull User user,
      @NotNull Alcohol alcohol,
      @NotNull String content,
      @NotNull SizeType sizeType,
      @NotNull BigDecimal price) {
    Review review =
        Review.builder()
            .userId(user.getId())
            .alcoholId(alcohol.getId())
            .content(content)
            .sizeType(sizeType)
            .price(price)
            .status(ReviewDisplayStatus.PUBLIC)
            .activeStatus(ReviewActiveStatus.ACTIVE)
            .build();
    em.persist(review);
    em.flush();
    return review;
  }

  /** 빌더를 통한 Review 생성 - 누락 필드 자동 채우기 */
  @Transactional
  @NotNull
  public Review persistReview(@NotNull Review.ReviewBuilder builder) {
    // 누락 필드 채우기
    Review.ReviewBuilder filledBuilder = fillMissingReviewFields(builder);
    Review review = filledBuilder.build();
    em.persist(review);
    em.flush();
    return review;
  }

  /** 여러 Review 일괄 생성 */
  @Transactional
  @NotNull
  public List<Review> persistReviews(@NotNull User user, @NotNull Alcohol alcohol, int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> persistReview(user, alcohol))
        .toList();
  }

  // ========== ReviewReply 생성 메서드들 ==========

  /** 기본 ReviewReply 생성 (Review, User 객체 사용) */
  @Transactional
  @NotNull
  public ReviewReply persistReviewReply(@NotNull Review review, @NotNull User author) {
    ReviewReply reply =
        ReviewReply.builder()
            .reviewId(review.getId())
            .userId(author.getId())
            .content("좋은 리뷰 감사합니다! " + generateRandomSuffix())
            .status(ReviewReplyStatus.NORMAL)
            .build();
    em.persist(reply);
    em.flush();
    return reply;
  }

  /** ID로 ReviewReply 생성 */
  @Transactional
  @NotNull
  public ReviewReply persistReviewReply(@NotNull Long reviewId, @NotNull Long userId) {
    ReviewReply reply =
        ReviewReply.builder()
            .reviewId(reviewId)
            .userId(userId)
            .content("좋은 리뷰 감사합니다! " + generateRandomSuffix())
            .status(ReviewReplyStatus.NORMAL)
            .build();
    em.persist(reply);
    em.flush();
    return reply;
  }

  /** 커스텀 내용으로 ReviewReply 생성 */
  @Transactional
  @NotNull
  public ReviewReply persistReviewReply(
      @NotNull Review review, @NotNull User author, @NotNull String content) {
    ReviewReply reply =
        ReviewReply.builder()
            .reviewId(review.getId())
            .userId(author.getId())
            .content(content)
            .status(ReviewReplyStatus.NORMAL)
            .build();
    em.persist(reply);
    em.flush();
    return reply;
  }

  /** 대댓글 생성 (부모 댓글 지정) */
  @Transactional
  @NotNull
  public ReviewReply persistReviewReply(
      @NotNull Review review,
      @NotNull User author,
      @NotNull ReviewReply parentReply,
      @NotNull String content) {
    ReviewReply reply =
        ReviewReply.builder()
            .reviewId(review.getId())
            .userId(author.getId())
            .content(content)
            .status(ReviewReplyStatus.NORMAL)
            .parentReviewReply(parentReply)
            .rootReviewReply(
                parentReply.getRootReviewReply() != null
                    ? parentReply.getRootReviewReply()
                    : parentReply)
            .build();
    em.persist(reply);
    em.flush();
    return reply;
  }

  /** 빌더를 통한 ReviewReply 생성 - 누락 필드 자동 채우기 */
  @Transactional
  @NotNull
  public ReviewReply persistReviewReply(@NotNull ReviewReply.ReviewReplyBuilder builder) {
    // 누락 필드 채우기
    ReviewReply.ReviewReplyBuilder filledBuilder = fillMissingReviewReplyFields(builder);
    ReviewReply reply = filledBuilder.build();
    em.persist(reply);
    em.flush();
    return reply;
  }

  // ========== ReviewImage 생성 메서드들 ==========

  /** 기본 ReviewImage 생성 */
  @Transactional
  @NotNull
  public ReviewImage persistReviewImage(@NotNull Review review, @NotNull String imageUrl) {
    ImageInfo imageInfo =
        ImageInfo.builder()
            .order(1)
            .imageUrl(imageUrl)
            .imagePath("/reviews/" + review.getId())
            .imageKey("review-" + review.getId() + "-" + generateRandomSuffix())
            .imageName("review-image-" + generateRandomSuffix() + ".jpg")
            .build();

    ReviewImage reviewImage =
        ReviewImage.builder().reviewImageInfo(imageInfo).review(review).build();

    em.persist(reviewImage);
    em.flush();
    return reviewImage;
  }

  /** ImageInfo를 직접 지정하여 ReviewImage 생성 */
  @Transactional
  @NotNull
  public ReviewImage persistReviewImage(@NotNull Review review, @NotNull ImageInfo imageInfo) {
    ReviewImage reviewImage =
        ReviewImage.builder().reviewImageInfo(imageInfo).review(review).build();

    em.persist(reviewImage);
    em.flush();
    return reviewImage;
  }

  /** 여러 ReviewImage 일괄 생성 */
  @Transactional
  @NotNull
  public List<ReviewImage> persistReviewImages(@NotNull Review review, int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(
            i -> {
              String imageUrl = "https://example.com/review-image-" + i + ".jpg";
              return persistReviewImage(review, imageUrl);
            })
        .toList();
  }

  // ========== 헬퍼 메서드들 ==========

  /** 랜덤 접미사 생성 헬퍼 메서드 */
  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  /** Review 빌더의 누락 필드 채우기 */
  private Review.ReviewBuilder fillMissingReviewFields(Review.ReviewBuilder builder) {
    // 빌더를 임시로 빌드해서 필드 체크
    Review tempReview;
    try {
      tempReview = builder.build();
    } catch (Exception e) {
      // 필수 필드 누락 시 기본값으로 채우기
      throw new IllegalArgumentException(
          "Review 생성을 위해서는 userId, alcoholId, content, sizeType, price가 필요합니다.", e);
    }

    // 개별 필드 체크 및 채우기
    if (tempReview.getUserId() == null) {
      throw new IllegalArgumentException("Review 생성을 위해 userId가 필요합니다.");
    }
    if (tempReview.getAlcoholId() == null) {
      throw new IllegalArgumentException("Review 생성을 위해 alcoholId가 필요합니다.");
    }
    if (tempReview.getContent() == null || tempReview.getContent().isEmpty()) {
      builder.content("기본 리뷰 내용 " + generateRandomSuffix());
    }
    if (tempReview.getSizeType() == null) {
      builder.sizeType(SizeType.BOTTLE);
    }
    if (tempReview.getPrice() == null) {
      builder.price(BigDecimal.valueOf(50000));
    }
    if (tempReview.getStatus() == null) {
      builder.status(ReviewDisplayStatus.PUBLIC);
    }
    if (tempReview.getActiveStatus() == null) {
      builder.activeStatus(ReviewActiveStatus.ACTIVE);
    }

    return builder;
  }

  /** ReviewReply 빌더의 누락 필드 채우기 */
  private ReviewReply.ReviewReplyBuilder fillMissingReviewReplyFields(
      ReviewReply.ReviewReplyBuilder builder) {
    // 빌더를 임시로 빌드해서 필드 체크
    ReviewReply tempReply;
    try {
      tempReply = builder.build();
    } catch (Exception e) {
      // 필수 필드 누락 시 기본값으로 채우기
      throw new IllegalArgumentException("ReviewReply 생성을 위해 reviewId, userId, content가 필요합니다.", e);
    }

    // 개별 필드 체크 및 채우기
    if (tempReply.getReviewId() == null) {
      throw new IllegalArgumentException("ReviewReply 생성을 위해 reviewId가 필요합니다.");
    }
    if (tempReply.getUserId() == null) {
      throw new IllegalArgumentException("ReviewReply 생성을 위해 userId가 필요합니다.");
    }
    if (tempReply.getContent() == null || tempReply.getContent().isEmpty()) {
      builder.content("기본 댓글 내용 " + generateRandomSuffix());
    }
    if (tempReply.getStatus() == null) {
      builder.status(ReviewReplyStatus.NORMAL);
    }

    return builder;
  }
}
