package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservation;
import app.bottlenote.alcohols.domain.AlcoholInterestObservation;
import app.bottlenote.alcohols.domain.AlcoholPickObservation;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshot;
import app.bottlenote.alcohols.domain.AlcoholRatingObservation;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 인기도 Snapshot과 축별 관측 행을 통합 테스트 DB에 저장한다. */
@RequiredArgsConstructor
@Component
public class AlcoholPopularityTestFactory {

  private final EntityManager em;

  @Transactional
  @NotNull
  public AlcoholPopularitySnapshot persistSnapshot(
      @NotNull Long alcoholId,
      @NotNull BucketGranularity granularity,
      @NotNull LocalDateTime bucketAt,
      @NotNull Long interestValue,
      @NotNull Long ratingValue,
      @NotNull Long pickValue,
      @NotNull Long engagementValue,
      @NotNull BigDecimal interestScore,
      @NotNull BigDecimal ratingScore,
      @NotNull BigDecimal pickScore,
      @NotNull BigDecimal engagementScore,
      @NotNull BigDecimal popularityScore) {
    AlcoholPopularitySnapshot snapshot =
        AlcoholPopularitySnapshot.builder()
            .alcoholId(alcoholId)
            .bucketGranularity(granularity)
            .bucketAt(bucketAt)
            .observedAt(bucketAt.plusHours(1))
            .interestValue(interestValue)
            .interestSourceBucketAt(bucketAt)
            .interestScore(interestScore)
            .ratingValue(ratingValue)
            .ratingSourceBucketAt(bucketAt)
            .ratingScore(ratingScore)
            .pickValue(pickValue)
            .pickSourceBucketAt(bucketAt)
            .pickScore(pickScore)
            .engagementValue(engagementValue)
            .engagementSourceBucketAt(bucketAt)
            .engagementScore(engagementScore)
            .popularityScore(popularityScore)
            .build();
    em.persist(snapshot);
    em.flush();
    return snapshot;
  }

  @Transactional
  @NotNull
  public AlcoholInterestObservation persistInterest(
      @NotNull Long alcoholId,
      @NotNull BucketGranularity granularity,
      @NotNull LocalDateTime bucketAt,
      @NotNull Long viewCount,
      @NotNull Long cumulativeViewCount) {
    AlcoholInterestObservation observation =
        AlcoholInterestObservation.builder()
            .alcoholId(alcoholId)
            .bucketGranularity(granularity)
            .bucketAt(bucketAt)
            .observedAt(bucketAt.plusHours(1))
            .viewCount(viewCount)
            .cumulativeViewCount(cumulativeViewCount)
            .build();
    em.persist(observation);
    em.flush();
    return observation;
  }

  @Transactional
  @NotNull
  public AlcoholRatingObservation persistRating(
      @NotNull Long alcoholId,
      @NotNull BucketGranularity granularity,
      @NotNull LocalDateTime bucketAt,
      @NotNull Long ratingCount,
      @NotNull BigDecimal ratingSum,
      @NotNull Long deltaRatingCount,
      @NotNull BigDecimal deltaRatingSum) {
    AlcoholRatingObservation observation =
        AlcoholRatingObservation.builder()
            .alcoholId(alcoholId)
            .bucketGranularity(granularity)
            .bucketAt(bucketAt)
            .observedAt(bucketAt.plusHours(1))
            .ratingCount(ratingCount)
            .ratingSum(ratingSum)
            .deltaRatingCount(deltaRatingCount)
            .deltaRatingSum(deltaRatingSum)
            .build();
    em.persist(observation);
    em.flush();
    return observation;
  }

  @Transactional
  @NotNull
  public AlcoholPickObservation persistPick(
      @NotNull Long alcoholId,
      @NotNull BucketGranularity granularity,
      @NotNull LocalDateTime bucketAt,
      @NotNull Long pickCount,
      @NotNull Long unpickCount,
      @NotNull Long deltaPickCount) {
    AlcoholPickObservation observation =
        AlcoholPickObservation.builder()
            .alcoholId(alcoholId)
            .bucketGranularity(granularity)
            .bucketAt(bucketAt)
            .observedAt(bucketAt.plusHours(1))
            .pickCount(pickCount)
            .unpickCount(unpickCount)
            .deltaPickCount(deltaPickCount)
            .build();
    em.persist(observation);
    em.flush();
    return observation;
  }

  @Transactional
  @NotNull
  public AlcoholEngagementObservation persistEngagement(
      @NotNull Long alcoholId,
      @NotNull BucketGranularity granularity,
      @NotNull LocalDateTime bucketAt,
      @NotNull Long reviewCount,
      @NotNull Long likeCount,
      @NotNull Long dislikeCount,
      @NotNull Long replyCount,
      @NotNull Long deltaReviewCount,
      @NotNull Long deltaLikeCount,
      @NotNull Long deltaDislikeCount,
      @NotNull Long deltaReplyCount) {
    AlcoholEngagementObservation observation =
        AlcoholEngagementObservation.builder()
            .alcoholId(alcoholId)
            .bucketGranularity(granularity)
            .bucketAt(bucketAt)
            .observedAt(bucketAt.plusHours(1))
            .reviewCount(reviewCount)
            .likeCount(likeCount)
            .dislikeCount(dislikeCount)
            .replyCount(replyCount)
            .deltaReviewCount(deltaReviewCount)
            .deltaLikeCount(deltaLikeCount)
            .deltaDislikeCount(deltaDislikeCount)
            .deltaReplyCount(deltaReplyCount)
            .build();
    em.persist(observation);
    em.flush();
    return observation;
  }
}
