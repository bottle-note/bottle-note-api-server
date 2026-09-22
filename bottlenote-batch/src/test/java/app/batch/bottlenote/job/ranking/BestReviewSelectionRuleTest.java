package app.batch.bottlenote.job.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("batch")
@DisplayName("[batch] 베스트 리뷰 선정 규칙")
class BestReviewSelectionRuleTest {

  @Test
  @DisplayName("순좋아요 0.6, 댓글 작성자 0.2, 이미지 보유 0.2로 점수를 계산한다")
  void scoresWithFixedWeights() {
    assertThat(BestReviewSelectionRule.score(5, 1, 2, 3)).isEqualByComparingTo("3.00");
    assertThat(BestReviewSelectionRule.score(0, 0, 0, 0)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("싫어요는 좋아요를 그대로 상쇄한다")
  void dislikeOffsetsLike() {
    assertThat(BestReviewSelectionRule.score(5, 10, 0, 0)).isEqualByComparingTo("-3.00");
    assertThat(BestReviewSelectionRule.netLikes(5, 10)).isEqualTo(-5L);
  }

  @Test
  @DisplayName("댓글 작성자 수는 5명까지만 반영한다")
  void repliersAreCapped() {
    assertThat(BestReviewSelectionRule.score(0, 0, 5, 0))
        .isEqualByComparingTo(BestReviewSelectionRule.score(0, 0, 50, 0))
        .isEqualByComparingTo("1.00");
  }

  @Test
  @DisplayName("이미지는 개수와 무관하게 보유 여부로만 가산한다")
  void imageBonusIsFlat() {
    assertThat(BestReviewSelectionRule.score(0, 0, 0, 1))
        .isEqualByComparingTo(BestReviewSelectionRule.score(0, 0, 0, 7))
        .isEqualByComparingTo("0.20");
  }

  @Test
  @DisplayName("주류 리뷰 2건 이상, 순좋아요 1 이상, 본문 30자 이상을 모두 만족해야 후보다")
  void eligibilityRequiresAllConditions() {
    assertThat(BestReviewSelectionRule.isEligible(2, 1, 0, 30)).isTrue();
    assertThat(BestReviewSelectionRule.isEligible(1, 9, 0, 100)).isFalse();
    assertThat(BestReviewSelectionRule.isEligible(5, 3, 3, 100)).isFalse();
    assertThat(BestReviewSelectionRule.isEligible(5, 3, 0, 29)).isFalse();
  }

  @Test
  @DisplayName("리뷰 수 구간별로 1·2·3건을 선정한다")
  void allowedCountByTier() {
    assertThat(BestReviewSelectionRule.allowedCount(9)).isEqualTo(1);
    assertThat(BestReviewSelectionRule.allowedCount(10)).isEqualTo(2);
    assertThat(BestReviewSelectionRule.allowedCount(19)).isEqualTo(2);
    assertThat(BestReviewSelectionRule.allowedCount(20)).isEqualTo(3);

    assertThat(BestReviewSelectionRule.tierRuleCode(9))
        .isEqualTo(BestReviewSelectionRule.RULE_UNDER_10_TOP_1);
    assertThat(BestReviewSelectionRule.tierRuleCode(10))
        .isEqualTo(BestReviewSelectionRule.RULE_10_TO_19_TOP_2);
    assertThat(BestReviewSelectionRule.tierRuleCode(20))
        .isEqualTo(BestReviewSelectionRule.RULE_20_OVER_TOP_3);
  }
}
