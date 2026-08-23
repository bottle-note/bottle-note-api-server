package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** HOUR 관측 원본을 WEEK 또는 MONTH 관측으로 조밀하게 롤업한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularityRollupTasklet implements Tasklet {

  public static final String GRANULARITY_PARAM = "observationGranularity";

  private static final String INTEREST_SQL =
      """
      INSERT INTO alcohol_interest_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         view_count, cumulative_view_count)
      SELECT a.id, ?, ?, ?, previous.bucket_at,
             COALESCE(period_value.view_count, 0),
             COALESCE(latest.cumulative_view_count, 0)
      FROM alcohols a
      LEFT JOIN (
        SELECT alcohol_id, SUM(view_count) AS view_count
        FROM alcohol_interest_observations
        WHERE bucket_granularity = 'HOUR' AND bucket_at >= ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) period_value ON period_value.alcohol_id = a.id
      LEFT JOIN (
        SELECT source.alcohol_id, source.cumulative_view_count
        FROM alcohol_interest_observations source
        JOIN (
          SELECT alcohol_id, MAX(bucket_at) AS bucket_at
          FROM alcohol_interest_observations
          WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
          GROUP BY alcohol_id
        ) boundary
          ON boundary.alcohol_id = source.alcohol_id AND boundary.bucket_at = source.bucket_at
        WHERE source.bucket_granularity = 'HOUR'
      ) latest ON latest.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id, MAX(bucket_at) AS bucket_at
        FROM alcohol_interest_observations
        WHERE bucket_granularity = ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) previous ON previous.alcohol_id = a.id
      WHERE a.deleted_at IS NULL
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        view_count = VALUES(view_count),
        cumulative_view_count = VALUES(cumulative_view_count)
      """;

  private static final String RATING_SQL =
      """
      INSERT INTO alcohol_rating_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         rating_count, rating_sum, delta_rating_count, delta_rating_sum)
      SELECT a.id, ?, ?, ?, previous.bucket_at,
             COALESCE(latest.rating_count, 0), COALESCE(latest.rating_sum, 0),
             COALESCE(period_delta.rating_count, 0), COALESCE(period_delta.rating_sum, 0)
      FROM alcohols a
      LEFT JOIN (
        SELECT source.alcohol_id, source.rating_count, source.rating_sum
        FROM alcohol_rating_observations source
        JOIN (
          SELECT alcohol_id, MAX(bucket_at) AS bucket_at
          FROM alcohol_rating_observations
          WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
          GROUP BY alcohol_id
        ) boundary
          ON boundary.alcohol_id = source.alcohol_id AND boundary.bucket_at = source.bucket_at
        WHERE source.bucket_granularity = 'HOUR'
      ) latest ON latest.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id,
               SUM(delta_rating_count) AS rating_count,
               SUM(delta_rating_sum) AS rating_sum
        FROM alcohol_rating_observations
        WHERE bucket_granularity = 'HOUR' AND bucket_at >= ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) period_delta ON period_delta.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id, MAX(bucket_at) AS bucket_at
        FROM alcohol_rating_observations
        WHERE bucket_granularity = ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) previous ON previous.alcohol_id = a.id
      WHERE a.deleted_at IS NULL
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        rating_count = VALUES(rating_count),
        rating_sum = VALUES(rating_sum),
        delta_rating_count = VALUES(delta_rating_count),
        delta_rating_sum = VALUES(delta_rating_sum)
      """;

  private static final String PICK_SQL =
      """
      INSERT INTO alcohol_pick_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         pick_count, unpick_count, delta_pick_count)
      SELECT a.id, ?, ?, ?, previous.bucket_at,
             COALESCE(latest.pick_count, 0), COALESCE(latest.unpick_count, 0),
             COALESCE(period_delta.pick_count, 0)
      FROM alcohols a
      LEFT JOIN (
        SELECT source.alcohol_id, source.pick_count, source.unpick_count
        FROM alcohol_pick_observations source
        JOIN (
          SELECT alcohol_id, MAX(bucket_at) AS bucket_at
          FROM alcohol_pick_observations
          WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
          GROUP BY alcohol_id
        ) boundary
          ON boundary.alcohol_id = source.alcohol_id AND boundary.bucket_at = source.bucket_at
        WHERE source.bucket_granularity = 'HOUR'
      ) latest ON latest.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id, SUM(delta_pick_count) AS pick_count
        FROM alcohol_pick_observations
        WHERE bucket_granularity = 'HOUR' AND bucket_at >= ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) period_delta ON period_delta.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id, MAX(bucket_at) AS bucket_at
        FROM alcohol_pick_observations
        WHERE bucket_granularity = ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) previous ON previous.alcohol_id = a.id
      WHERE a.deleted_at IS NULL
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        pick_count = VALUES(pick_count),
        unpick_count = VALUES(unpick_count),
        delta_pick_count = VALUES(delta_pick_count)
      """;

  private static final String ENGAGEMENT_SQL =
      """
      INSERT INTO alcohol_engagement_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         review_count, like_count, dislike_count, reply_count,
         delta_review_count, delta_like_count, delta_dislike_count, delta_reply_count)
      SELECT a.id, ?, ?, ?, previous.bucket_at,
             COALESCE(latest.review_count, 0), COALESCE(latest.like_count, 0),
             COALESCE(latest.dislike_count, 0), COALESCE(latest.reply_count, 0),
             COALESCE(period_delta.review_count, 0), COALESCE(period_delta.like_count, 0),
             COALESCE(period_delta.dislike_count, 0), COALESCE(period_delta.reply_count, 0)
      FROM alcohols a
      LEFT JOIN (
        SELECT source.alcohol_id, source.review_count, source.like_count,
               source.dislike_count, source.reply_count
        FROM alcohol_engagement_observations source
        JOIN (
          SELECT alcohol_id, MAX(bucket_at) AS bucket_at
          FROM alcohol_engagement_observations
          WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
          GROUP BY alcohol_id
        ) boundary
          ON boundary.alcohol_id = source.alcohol_id AND boundary.bucket_at = source.bucket_at
        WHERE source.bucket_granularity = 'HOUR'
      ) latest ON latest.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id,
               SUM(delta_review_count) AS review_count,
               SUM(delta_like_count) AS like_count,
               SUM(delta_dislike_count) AS dislike_count,
               SUM(delta_reply_count) AS reply_count
        FROM alcohol_engagement_observations
        WHERE bucket_granularity = 'HOUR' AND bucket_at >= ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) period_delta ON period_delta.alcohol_id = a.id
      LEFT JOIN (
        SELECT alcohol_id, MAX(bucket_at) AS bucket_at
        FROM alcohol_engagement_observations
        WHERE bucket_granularity = ? AND bucket_at < ?
        GROUP BY alcohol_id
      ) previous ON previous.alcohol_id = a.id
      WHERE a.deleted_at IS NULL
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        review_count = VALUES(review_count),
        like_count = VALUES(like_count),
        dislike_count = VALUES(dislike_count),
        reply_count = VALUES(reply_count),
        delta_review_count = VALUES(delta_review_count),
        delta_like_count = VALUES(delta_like_count),
        delta_dislike_count = VALUES(delta_dislike_count),
        delta_reply_count = VALUES(delta_reply_count)
      """;

  private final ObservationWriter writer;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    ObservationBucket bucket = bucketFrom(parameters);
    LocalDateTime observedAt = LocalDateTime.now();
    Integer targets =
        writer
            .jdbc()
            .queryForObject(
                "SELECT COUNT(*) FROM alcohols WHERE deleted_at IS NULL", Integer.class);

    rollupInterest(bucket, observedAt);
    rollupState(RATING_SQL, bucket, observedAt);
    rollupState(PICK_SQL, bucket, observedAt);
    rollupState(ENGAGEMENT_SQL, bucket, observedAt);

    int written = Math.multiplyExact(targets == null ? 0 : targets, 4);
    contribution.incrementWriteCount(written);
    log.info(
        "인기도 관측 롤업 완료. bucketAt={}, granularity={}, 영향={}행",
        bucket.startAt(),
        bucket.granularity(),
        written);
    return RepeatStatus.FINISHED;
  }

  private ObservationBucket bucketFrom(JobParameters parameters) {
    String value = parameters == null ? null : parameters.getString(GRANULARITY_PARAM);
    LocalDateTime bucketAt =
        parameters == null ? null : parameters.getLocalDateTime(ObservationBucket.BUCKET_AT_PARAM);
    if (value == null || bucketAt == null) {
      throw new IllegalArgumentException("롤업 단위와 버킷 시작 시각은 필수입니다.");
    }

    BucketGranularity granularity;
    try {
      granularity = BucketGranularity.valueOf(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("지원하지 않는 롤업 단위입니다: " + value, exception);
    }
    if (granularity == BucketGranularity.HOUR) {
      throw new IllegalArgumentException("HOUR는 롤업할 수 없습니다.");
    }
    return new ObservationBucket(granularity, bucketAt);
  }

  private void rollupInterest(ObservationBucket bucket, LocalDateTime observedAt) {
    writer
        .jdbc()
        .update(
            INTEREST_SQL,
            bucket.granularity().name(),
            bucket.startAt(),
            observedAt,
            bucket.startAt(),
            bucket.endAt(),
            bucket.endAt(),
            bucket.granularity().name(),
            bucket.startAt());
  }

  private void rollupState(String sql, ObservationBucket bucket, LocalDateTime observedAt) {
    writer
        .jdbc()
        .update(
            sql,
            bucket.granularity().name(),
            bucket.startAt(),
            observedAt,
            bucket.endAt(),
            bucket.startAt(),
            bucket.endAt(),
            bucket.granularity().name(),
            bucket.startAt());
  }
}
