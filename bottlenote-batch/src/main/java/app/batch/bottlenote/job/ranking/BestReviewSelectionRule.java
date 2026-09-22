package app.batch.bottlenote.job.ranking;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 베스트 리뷰 선정 규칙.
 *
 * <p>적재 경로와 떼어낸 순수 함수다. DB 없이 검증할 수 있어야 한다.
 *
 * <p>2026.09.22 운영 데이터 기준으로 옛 규칙을 손봤다. 혼자 있는 리뷰는 베스트라 부르지 않고, 싫어요는 순좋아요로 상쇄하며, 댓글은 작성자 본인을 뺀 서로
 * 다른 사람 수로 상한을 두고 센다. 동점은 최신 리뷰가 이긴다.
 */
public final class BestReviewSelectionRule {

  public static final int SCALE = 2;

  /** 비교 대상이 있어야 베스트다. */
  public static final int MIN_ALCOHOL_REVIEWS = 2;

  /** 좋아요에서 싫어요를 뺀 값이 이만큼은 있어야 후보다. */
  public static final int MIN_NET_LIKES = 1;

  /** 본문이 이보다 짧으면 후보에서 뺀다. */
  public static final int MIN_CONTENT_LENGTH = 30;

  /** 댓글 작성자 수는 이 값까지만 점수에 반영한다. */
  public static final int MAX_COUNTED_REPLIERS = 5;

  public static final String RULE_UNDER_10_TOP_1 = "REVIEW_COUNT_UNDER_10_TOP_1";
  public static final String RULE_10_TO_19_TOP_2 = "REVIEW_COUNT_10_TO_19_TOP_2";
  public static final String RULE_20_OVER_TOP_3 = "REVIEW_COUNT_20_OVER_TOP_3";
  public static final String RULE_ELIGIBILITY_NOT_MET = "ELIGIBILITY_NOT_MET";
  public static final String RULE_NOT_ACTIVE_PUBLIC = "NOT_ACTIVE_PUBLIC";

  private static final BigDecimal NET_LIKE_WEIGHT = new BigDecimal("0.6");
  private static final BigDecimal REPLIER_WEIGHT = new BigDecimal("0.2");
  private static final BigDecimal IMAGE_BONUS = new BigDecimal("0.2");

  private BestReviewSelectionRule() {}

  public static long netLikes(long likeCount, long dislikeCount) {
    return likeCount - dislikeCount;
  }

  /** 순좋아요 0.6 + 댓글 작성자 수(최대 5) 0.2 + 이미지 보유 0.2. 순좋아요가 음수면 그대로 감점된다. */
  public static BigDecimal score(
      long likeCount, long dislikeCount, long replierCount, long imageCount) {
    long countedRepliers = Math.min(Math.max(replierCount, 0L), MAX_COUNTED_REPLIERS);
    BigDecimal score =
        NET_LIKE_WEIGHT
            .multiply(BigDecimal.valueOf(netLikes(likeCount, dislikeCount)))
            .add(REPLIER_WEIGHT.multiply(BigDecimal.valueOf(countedRepliers)))
            .add(imageCount > 0 ? IMAGE_BONUS : BigDecimal.ZERO);
    return score.setScale(SCALE, RoundingMode.HALF_UP);
  }

  /** 주류 리뷰 2건 이상, 순좋아요 1 이상, 본문 30자 이상을 모두 만족해야 후보다. */
  public static boolean isEligible(
      long alcoholReviewCount, long likeCount, long dislikeCount, long contentLength) {
    return alcoholReviewCount >= MIN_ALCOHOL_REVIEWS
        && netLikes(likeCount, dislikeCount) >= MIN_NET_LIKES
        && contentLength >= MIN_CONTENT_LENGTH;
  }

  /** 주류 리뷰 수 구간별로 선정할 수 있는 리뷰 수. */
  public static int allowedCount(long alcoholReviewCount) {
    if (alcoholReviewCount >= 20) {
      return 3;
    }
    if (alcoholReviewCount >= 10) {
      return 2;
    }
    return 1;
  }

  public static String tierRuleCode(long alcoholReviewCount) {
    if (alcoholReviewCount >= 20) {
      return RULE_20_OVER_TOP_3;
    }
    if (alcoholReviewCount >= 10) {
      return RULE_10_TO_19_TOP_2;
    }
    return RULE_UNDER_10_TOP_1;
  }
}
