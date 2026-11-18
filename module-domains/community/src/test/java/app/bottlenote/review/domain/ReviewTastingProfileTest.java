package app.bottlenote.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.review.exception.ReviewException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("ReviewTastingProfile")
class ReviewTastingProfileTest {

  @Test
  @DisplayName("FE가 지정한 축 수와 점수 상한을 그대로 저장한다")
  void FE가_지정한_축_수와_점수_상한을_그대로_저장한다() {
    ReviewTastingProfile profile =
        ReviewTastingProfile.normalize(
            new ReviewTastingProfile(
                1,
                10,
                List.of(
                    new ReviewTastingProfile.Axis("SMOKY", " 스모키 ", " 연기 ", 3),
                    new ReviewTastingProfile.Axis(null, "과일", "  ", 0))));

    assertThat(profile.maxScore()).isEqualTo(10);
    assertThat(profile.axes()).hasSize(2);
    assertThat(profile.axes().get(0))
        .isEqualTo(new ReviewTastingProfile.Axis("SMOKY", "스모키", "연기", 3));
    assertThat(profile.axes().get(1)).isEqualTo(new ReviewTastingProfile.Axis(null, "과일", null, 0));
  }

  @Test
  @DisplayName("모든 점수가 0이면 미기록으로 정규화한다")
  void 모든_점수가_0이면_미기록으로_정규화한다() {
    ReviewTastingProfile profile =
        ReviewTastingProfile.normalize(
            new ReviewTastingProfile(
                1, 5, List.of(new ReviewTastingProfile.Axis("SMOKY", "스모키", null, 0))));

    assertThat(profile).isNull();
  }

  @Test
  @DisplayName("지원하지 않는 버전이나 범위를 벗어나면 거절한다")
  void 지원하지_않는_버전이나_범위를_벗어나면_거절한다() {
    ReviewTastingProfile invalid =
        new ReviewTastingProfile(
            2, 5, List.of(new ReviewTastingProfile.Axis("SMOKY", "스모키", null, 1)));

    assertThatThrownBy(() -> ReviewTastingProfile.normalize(invalid))
        .isInstanceOf(ReviewException.class);
  }
}
