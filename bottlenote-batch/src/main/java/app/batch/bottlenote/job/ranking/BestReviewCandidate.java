package app.batch.bottlenote.job.ranking;

import java.math.BigDecimal;

/** 판정 시점의 리뷰 한 건. 순위는 같은 주류 안에서 점수 내림차순, 동점이면 최신 리뷰(ID 내림차순)가 앞선다. */
public record BestReviewCandidate(
    long reviewId,
    long alcoholId,
    long likeCount,
    long dislikeCount,
    long replierCount,
    long imageCount,
    long contentLength,
    long alcoholReviewCount,
    BigDecimal score,
    int ranking) {

  public long netLikes() {
    return BestReviewSelectionRule.netLikes(likeCount, dislikeCount);
  }

  public boolean isEligible() {
    return BestReviewSelectionRule.isEligible(
        alcoholReviewCount, likeCount, dislikeCount, contentLength);
  }

  public boolean isWithinAllowedRank() {
    return ranking <= BestReviewSelectionRule.allowedCount(alcoholReviewCount);
  }

  public boolean isSelected() {
    return isEligible() && isWithinAllowedRank();
  }

  public String tierRuleCode() {
    return BestReviewSelectionRule.tierRuleCode(alcoholReviewCount);
  }

  public BestReviewCandidate withRanking(int newRanking) {
    return new BestReviewCandidate(
        reviewId,
        alcoholId,
        likeCount,
        dislikeCount,
        replierCount,
        imageCount,
        contentLength,
        alcoholReviewCount,
        score,
        newRanking);
  }
}
