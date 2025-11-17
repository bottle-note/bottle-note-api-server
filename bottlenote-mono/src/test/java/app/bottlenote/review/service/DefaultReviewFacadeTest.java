package app.bottlenote.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultReviewFacade 단위 테스트")
class DefaultReviewFacadeTest {

  InMemoryReviewRepository reviewRepository;
  DefaultReviewFacade reviewFacade;

  @BeforeEach
  void setUp() {
    reviewRepository = new InMemoryReviewRepository();
    reviewFacade = new DefaultReviewFacade(reviewRepository);
  }

  // ========== getAlcoholIdByReviewId ==========

  @Test
  @DisplayName("리뷰 ID로 술 ID를 정확히 조회할 수 있다")
  void getAlcoholIdByReviewId_정상_조회() {
    // given
    Review review = createReview(1L, 100L, "훌륭한 위스키");
    reviewRepository.save(review);

    // when
    Long alcoholId = reviewFacade.getAlcoholIdByReviewId(review.getId());

    // then
    assertThat(alcoholId).isEqualTo(100L);
  }

  @Test
  @DisplayName("존재하지 않는 리뷰 ID일 때 REVIEW_NOT_FOUND 예외가 발생한다")
  void getAlcoholIdByReviewId_존재하지_않는_리뷰() {
    // given
    Long nonExistentReviewId = 999L;

    // when & then
    assertThatThrownBy(() -> reviewFacade.getAlcoholIdByReviewId(nonExistentReviewId))
        .isInstanceOf(ReviewException.class)
        .hasFieldOrPropertyWithValue("code", ReviewExceptionCode.REVIEW_NOT_FOUND);
  }

  @Test
  @DisplayName("여러 리뷰의 술 ID를 정확히 구분할 수 있다")
  void getAlcoholIdByReviewId_여러_리뷰_구분() {
    // given
    Review review1 = createReview(1L, 100L, "리뷰1");
    Review review2 = createReview(1L, 200L, "리뷰2");
    reviewRepository.save(review1);
    reviewRepository.save(review2);

    // when
    Long alcoholId1 = reviewFacade.getAlcoholIdByReviewId(review1.getId());
    Long alcoholId2 = reviewFacade.getAlcoholIdByReviewId(review2.getId());

    // then
    assertThat(alcoholId1).isEqualTo(100L);
    assertThat(alcoholId2).isEqualTo(200L);
  }

  // ========== isExistReview ==========

  @Test
  @DisplayName("리뷰가 존재할 때 true를 반환한다")
  void isExistReview_리뷰_존재() {
    // given
    Review review = createReview(1L, 100L, "존재하는 리뷰");
    reviewRepository.save(review);

    // when
    boolean exists = reviewFacade.isExistReview(review.getId());

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("리뷰가 존재하지 않을 때 false를 반환한다")
  void isExistReview_리뷰_미존재() {
    // given
    Long nonExistentReviewId = 999L;

    // when
    boolean exists = reviewFacade.isExistReview(nonExistentReviewId);

    // then
    assertThat(exists).isFalse();
  }

  // ========== requestBlockReview ==========

  @Test
  @DisplayName("리뷰를 정상적으로 차단할 수 있다")
  void requestBlockReview_정상_차단() {
    // given
    Review review = createReview(1L, 100L, "차단할 리뷰");
    reviewRepository.save(review);

    // when
    reviewFacade.requestBlockReview(review.getId());

    // then
    Review blocked = reviewRepository.findById(review.getId()).get();
    assertThat(blocked.getActiveStatus()).isEqualTo(ReviewActiveStatus.DISABLED);
  }

  @Test
  @DisplayName("존재하지 않는 리뷰 차단 시도 시 조용히 무시된다")
  void requestBlockReview_존재하지_않는_리뷰() {
    // given
    Long nonExistentReviewId = 999L;

    // when & then - 현재는 예외 발생하지 않음 (ifPresent 사용)
    assertThatCode(() -> reviewFacade.requestBlockReview(nonExistentReviewId))
        .doesNotThrowAnyException();

    // NOTE: 이것이 의도된 동작인지 확인 필요
    // 명시적으로 예외를 던지는 것이 더 안전할 수 있음
  }

  @Test
  @DisplayName("이미 차단된 리뷰 재차단 시 멱등성을 보장한다")
  void requestBlockReview_멱등성() {
    // given
    Review review = createReview(1L, 100L, "차단할 리뷰");
    reviewRepository.save(review);
    reviewFacade.requestBlockReview(review.getId());

    // when - 재차단
    reviewFacade.requestBlockReview(review.getId());

    // then - 여전히 차단 상태
    Review blocked = reviewRepository.findById(review.getId()).get();
    assertThat(blocked.getActiveStatus()).isEqualTo(ReviewActiveStatus.DISABLED);
  }

  // ========== Helper Methods ==========

  private Review createReview(Long userId, Long alcoholId, String content) {
    return Review.builder()
        .userId(userId)
        .alcoholId(alcoholId)
        .content(content)
        .sizeType(SizeType.BOTTLE)
        .price(BigDecimal.valueOf(50000))
        .status(ReviewDisplayStatus.PUBLIC)
        .build();
  }
}
