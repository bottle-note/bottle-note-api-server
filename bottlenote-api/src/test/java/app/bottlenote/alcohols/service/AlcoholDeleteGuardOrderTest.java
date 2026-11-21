package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholDeleteGuard;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.InMemoryRatingRepository;
import app.bottlenote.rating.service.RatingAlcoholDeleteGuard;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import app.bottlenote.review.service.ReviewAlcoholDeleteGuard;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] 주류 삭제 가드 순서")
class AlcoholDeleteGuardOrderTest {
  private static final Long USER_ID = 1L;

  private InMemoryAlcoholQueryRepository alcoholQueryRepository;
  private InMemoryReviewRepository reviewRepository;
  private InMemoryRatingRepository ratingRepository;
  private AdminAlcoholCommandService service;

  @BeforeEach
  void setUp() {
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    reviewRepository = new InMemoryReviewRepository();
    ratingRepository = new InMemoryRatingRepository();
    // 별점 가드를 앞에 두어도 선언된 순서(리뷰 먼저)로 확인하는지 검증한다.
    List<AlcoholDeleteGuard> reversedGuards =
        List.of(
            new RatingAlcoholDeleteGuard(ratingRepository),
            new ReviewAlcoholDeleteGuard(reviewRepository));
    service =
        new AdminAlcoholCommandService(
            alcoholQueryRepository,
            new InMemoryRegionRepository(),
            new InMemoryDistilleryRepository(),
            reversedGuards,
            new InMemoryAlcoholsTastingTagsRepository(),
            new InMemoryTastingTagRepository(),
            event -> {});
  }

  @Test
  @DisplayName("리뷰와 별점이 모두 있는 주류를 삭제할 때 리뷰 사유로 먼저 거절한다")
  void delete_리뷰와_별점이_모두_있으면_리뷰_사유() {
    // given
    Alcohol alcohol = saveAlcohol();
    saveReview(alcohol.getId());
    saveRating(alcohol.getId());

    // when & then
    assertThatThrownBy(() -> service.deleteAlcohol(alcohol.getId()))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_HAS_REVIEWS);
    assertThat(alcohol.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("별점만 있는 주류를 삭제할 때 별점 사유로 거절한다")
  void delete_별점만_있으면_별점_사유() {
    // given
    Alcohol alcohol = saveAlcohol();
    saveRating(alcohol.getId());

    // when & then
    assertThatThrownBy(() -> service.deleteAlcohol(alcohol.getId()))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_HAS_RATINGS);
    assertThat(alcohol.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("리뷰만 있는 주류를 삭제할 때 리뷰 사유로 거절한다")
  void delete_리뷰만_있으면_리뷰_사유() {
    // given
    Alcohol alcohol = saveAlcohol();
    saveReview(alcohol.getId());

    // when & then
    assertThatThrownBy(() -> service.deleteAlcohol(alcohol.getId()))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_HAS_REVIEWS);
    assertThat(alcohol.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("이미 삭제된 주류를 삭제할 때 리뷰가 있어도 이미 삭제됨 사유로 거절한다")
  void delete_이미_삭제된_주류는_가드보다_먼저_거절() {
    // given
    Alcohol alcohol = saveAlcohol();
    saveReview(alcohol.getId());
    alcohol.delete();

    // when & then
    assertThatThrownBy(() -> service.deleteAlcohol(alcohol.getId()))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_ALREADY_DELETED);
  }

  @Test
  @DisplayName("없는 주류를 삭제할 때 찾을 수 없음 사유로 거절한다")
  void delete_없는_주류() {
    // when & then
    assertThatThrownBy(() -> service.deleteAlcohol(999L))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
  }

  @Test
  @DisplayName("리뷰와 별점이 없는 주류를 삭제할 때 삭제 처리한다")
  void delete_참조가_없으면_삭제() {
    // given
    Alcohol alcohol = saveAlcohol();
    saveReview(alcohol.getId() + 1);
    saveRating(alcohol.getId() + 1);

    // when
    var result = service.deleteAlcohol(alcohol.getId());

    // then
    assertThat(result.targetId()).isEqualTo(alcohol.getId());
    assertThat(alcohol.isDeleted()).isTrue();
  }

  private Alcohol saveAlcohol() {
    return alcoholQueryRepository.save(
        Alcohol.builder()
            .korName("글렌피딕 12년")
            .engName("Glenfiddich 12")
            .type(AlcoholType.WHISKY)
            .korCategory("싱글 몰트")
            .engCategory("Single Malt")
            .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
            .build());
  }

  private void saveReview(Long alcoholId) {
    reviewRepository.save(
        Review.builder()
            .userId(USER_ID)
            .alcoholId(alcoholId)
            .content("삭제 가드 확인용 리뷰")
            .sizeType(SizeType.BOTTLE)
            .price(BigDecimal.valueOf(50000))
            .status(ReviewDisplayStatus.PUBLIC)
            .activeStatus(ReviewActiveStatus.ACTIVE)
            .build());
  }

  private void saveRating(Long alcoholId) {
    ratingRepository.save(
        Rating.builder()
            .id(RatingId.is(USER_ID, alcoholId))
            .ratingPoint(RatingPoint.of(5))
            .build());
  }
}
