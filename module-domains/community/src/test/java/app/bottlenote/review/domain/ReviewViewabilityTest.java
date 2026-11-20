package app.bottlenote.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("unit")
@DisplayName("리뷰 상세 열람 가능 여부")
class ReviewViewabilityTest {

  private static final Long AUTHOR_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;

  @Test
  @DisplayName("활성 공개 리뷰를 조회할 때 작성자가 아닌 사용자와 비회원도 볼 수 있다")
  void 활성_공개_리뷰는_누구나_볼_수_있다() {
    Review review = review(ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PUBLIC);

    assertThat(review.isViewableBy(OTHER_USER_ID)).isTrue();
    assertThat(review.isViewableBy(null)).isTrue();
  }

  @Test
  @DisplayName("활성 비공개 리뷰를 조회할 때 작성자만 볼 수 있다")
  void 활성_비공개_리뷰는_작성자만_볼_수_있다() {
    Review review = review(ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PRIVATE);

    assertThat(review.isViewableBy(AUTHOR_ID)).isTrue();
    assertThat(review.isViewableBy(OTHER_USER_ID)).isFalse();
    assertThat(review.isViewableBy(null)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(
      value = ReviewActiveStatus.class,
      names = {"DELETED", "DISABLED"})
  @DisplayName("삭제되거나 비활성인 리뷰를 조회할 때 작성자도 볼 수 없다")
  void 활성이_아닌_리뷰는_작성자도_볼_수_없다(ReviewActiveStatus activeStatus) {
    Review review = review(activeStatus, ReviewDisplayStatus.PUBLIC);

    assertThat(review.isViewableBy(AUTHOR_ID)).isFalse();
    assertThat(review.isViewableBy(OTHER_USER_ID)).isFalse();
  }

  private Review review(ReviewActiveStatus activeStatus, ReviewDisplayStatus displayStatus) {
    return Review.builder()
        .userId(AUTHOR_ID)
        .alcoholId(10L)
        .activeStatus(activeStatus)
        .status(displayStatus)
        .build();
  }
}
