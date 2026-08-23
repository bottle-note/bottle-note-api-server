package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 최종 인기도 적재.
 *
 * <p>네 축이 모두 관측된 뒤에만 실행된다. 한 축이라도 실패하면 이 Step은 돌지 않고, 최종 테이블이 갱신되지 않은 채 조회는 직전 버킷을 계속 본다.
 *
 * <p>대상은 어느 한 축이라도 관측 이력이 있는 주류의 합집합이다. 이번 버킷에 관측이 없는 축은 직전 값을 끌어오고, 그 값이 실제로 관측된 버킷을 함께 적재한다 —
 * 이것이 없으면 끌어온 값이 얼마나 묵은 것인지 알 수 없다.
 *
 * <p>정규화는 설정된 고정 기준으로 나눈다. 같은 버킷 안 다른 주류의 최댓값으로 나누면 자기 값이 그대로인데도 점수가 흔들려 시계열이 무의미해진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularitySnapshotTasklet implements Tasklet {

  private static final String TABLE = "alcohol_popularity_snapshots";

  private static final String INTEREST_SQL =
      latestSql("alcohol_interest_observations", "view_count");
  private static final String RATING_SQL =
      latestSql("alcohol_rating_observations", "rating_count");
  private static final String PICK_SQL = latestSql("alcohol_pick_observations", "pick_count");
  private static final String ENGAGEMENT_SQL =
      latestSql(
          "alcohol_engagement_observations", "(review_count + like_count + reply_count)");

  private static final String PREVIOUS_SNAPSHOT_SQL =
      """
      SELECT bucket_at
      FROM alcohol_popularity_snapshots
      WHERE bucket_granularity = ? AND bucket_at < ?
      ORDER BY bucket_at DESC
      LIMIT 1
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_popularity_snapshots
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         interest_value, interest_source_bucket_at, interest_score,
         rating_value, rating_source_bucket_at, rating_score,
         pick_value, pick_source_bucket_at, pick_score,
         engagement_value, engagement_source_bucket_at, engagement_score,
         popularity_score)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        interest_value = VALUES(interest_value),
        interest_source_bucket_at = VALUES(interest_source_bucket_at),
        interest_score = VALUES(interest_score),
        rating_value = VALUES(rating_value),
        rating_source_bucket_at = VALUES(rating_source_bucket_at),
        rating_score = VALUES(rating_score),
        pick_value = VALUES(pick_value),
        pick_source_bucket_at = VALUES(pick_source_bucket_at),
        pick_score = VALUES(pick_score),
        engagement_value = VALUES(engagement_value),
        engagement_source_bucket_at = VALUES(engagement_source_bucket_at),
        engagement_score = VALUES(engagement_score),
        popularity_score = VALUES(popularity_score)
      """;

  /**
   * 상태 축용. 이번 버킷 시점에서 각 주류의 최신 관측을 고른다.
   *
   * <p>희소 저장이라 이번 버킷이 아닐 수 있고, 그때는 직전 값이 여전히 유효하다 — 누적이기 때문이다.
   */
  private static String latestSql(String table, String valueExpression) {
    return """
        SELECT o.alcohol_id, o.bucket_at, %s AS observed_value
        FROM %s o
        JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
              FROM %s
              WHERE bucket_granularity = ? AND bucket_at <= ?
              GROUP BY alcohol_id) latest
          ON o.alcohol_id = latest.alcohol_id
         AND o.bucket_granularity = ?
         AND o.bucket_at = latest.max_bucket
        JOIN alcohols a ON a.id = o.alcohol_id AND a.deleted_at IS NULL
        """
        .formatted(valueExpression, table, table);
  }

  private final ObservationWriter writer;
  private final PopularityObservationProperties properties;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ObservationBucket bucket =
        bucketFrom(chunkContext.getStepContext().getStepExecution().getJobParameters());
    BucketGranularity granularity = bucket.granularity();
    LocalDateTime bucketAt = bucket.startAt();
    LocalDateTime observedAt = LocalDateTime.now();
    LocalDateTime previousBucketAt = findPreviousSnapshotBucket(granularity, bucketAt);

    Map<Long, Axis> interest = loadAxis(INTEREST_SQL, granularity, bucketAt);
    Map<Long, Axis> rating = loadAxis(RATING_SQL, granularity, bucketAt);
    Map<Long, Axis> pick = loadAxis(PICK_SQL, granularity, bucketAt);
    Map<Long, Axis> engagement = loadAxis(ENGAGEMENT_SQL, granularity, bucketAt);

    Set<Long> targets = new HashSet<>();
    targets.addAll(interest.keySet());
    targets.addAll(rating.keySet());
    targets.addAll(pick.keySet());
    targets.addAll(engagement.keySet());
    targets.addAll(writer.findAlcoholIdsAt(TABLE, granularity, bucketAt));

    var weights = properties.getWeights();
    var reference = properties.referenceFor(granularity);

    List<Object[]> rows = new ArrayList<>(targets.size());
    for (Long alcoholId : targets) {
      // 흐름 축은 구간 사건이라 직전 값을 끌어오지 않는다.
      // 다만 관측 이력은 대상 판정에 그대로 쓴다 — 조회가 없다고 목록에서 사라지면 정렬이 무너진다.
      Axis i = interest.getOrDefault(alcoholId, Axis.EMPTY);
      if (!bucketAt.equals(i.bucketAt())) {
        i = Axis.EMPTY;
      }
      Axis r = rating.getOrDefault(alcoholId, Axis.EMPTY);
      Axis p = pick.getOrDefault(alcoholId, Axis.EMPTY);
      Axis e = engagement.getOrDefault(alcoholId, Axis.EMPTY);

      BigDecimal interestScore =
          PopularityScoring.normalize(i.value(), reference.getInterest());
      BigDecimal ratingScore = PopularityScoring.normalize(r.value(), reference.getRating());
      BigDecimal pickScore = PopularityScoring.normalize(p.value(), reference.getPick());
      BigDecimal engagementScore =
          PopularityScoring.normalize(e.value(), reference.getEngagement());

      BigDecimal popularity =
          PopularityScoring.weightedSum(
              interestScore, weights.getInterest(),
              ratingScore, weights.getRating(),
              pickScore, weights.getPick(),
              engagementScore, weights.getEngagement());

      rows.add(
          new Object[] {
            alcoholId, granularity.name(), bucketAt, observedAt, previousBucketAt,
            i.value(), i.bucketAt(), interestScore,
            r.value(), r.bucketAt(), ratingScore,
            p.value(), p.bucketAt(), pickScore,
            e.value(), e.bucketAt(), engagementScore,
            popularity
          });
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info(
        "최종 인기도 적재 완료. bucketAt={}, granularity={}, 대상={}종",
        bucketAt,
        granularity,
        rows.size());
    return RepeatStatus.FINISHED;
  }

  private ObservationBucket bucketFrom(JobParameters parameters) {
    String value =
        parameters == null ? null : parameters.getString(PopularityRollupTasklet.GRANULARITY_PARAM);
    BucketGranularity granularity =
        value == null ? BucketGranularity.HOUR : BucketGranularity.valueOf(value);
    LocalDateTime bucketAt =
        parameters == null ? null : parameters.getLocalDateTime(ObservationBucket.BUCKET_AT_PARAM);
    if (bucketAt == null && granularity == BucketGranularity.HOUR) {
      bucketAt = ObservationBucket.from(parameters);
    }
    if (bucketAt == null) {
      throw new IllegalArgumentException("Snapshot 버킷 시작 시각은 필수입니다.");
    }
    return new ObservationBucket(granularity, bucketAt);
  }

  private LocalDateTime findPreviousSnapshotBucket(
      BucketGranularity granularity, LocalDateTime bucketAt) {
    return writer
        .jdbc()
        .query(
            PREVIOUS_SNAPSHOT_SQL,
            (rs, rowNumber) -> rs.getObject("bucket_at", LocalDateTime.class),
            granularity.name(),
            bucketAt)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private Map<Long, Axis> loadAxis(
      String sql, BucketGranularity granularity, LocalDateTime bucketAt) {
    Map<Long, Axis> result = new HashMap<>();
    writer
        .jdbc()
        .query(
            sql,
            rs -> {
              result.put(
                  rs.getLong("alcohol_id"),
                  new Axis(
                      rs.getLong("observed_value"),
                      rs.getObject("bucket_at", LocalDateTime.class)));
            },
            granularity.name(),
            bucketAt,
            granularity.name());
    return result;
  }

  /** 관측이 아예 없는 축. 값은 0이고 출처 버킷은 없다. */
  private record Axis(long value, LocalDateTime bucketAt) {
    static final Axis EMPTY = new Axis(0L, null);
  }
}
