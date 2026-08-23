package app.batch.bottlenote.job.popularity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 관심도 관측.
 *
 * <p>흐름 축이다. 원본(alcohols_view_histories)은 사용자당 한 행에 마지막 조회 시각만 남기므로 누적 조회수를 만들 수 없고, 관측 시점에 구간을 세는
 * 수밖에 없다. 세지 않고 지나간 구간은 복구되지 않는다.
 *
 * <p>구간은 [직전 관측 버킷, 이번 버킷)이다. 배치를 한 번 놓쳐도 다음 회차가 더 긴 구간을 세므로 손실이 줄어든다.
 *
 * <p>희소 저장 규칙이 다른 축과 다르다. 흐름 축은 값이 같아도 매 구간이 새로운 사건이라 "직전과 같으면 생략"을 적용하면 안 된다. 대신 조회가 없는 주류는 집계
 * 결과에 아예 나오지 않으므로 결과적으로 희소해진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterestObservationTasklet implements Tasklet {

  private static final String TABLE = "alcohol_interest_observations";

  /** 구간 상한. 이보다 오래 멈췄다면 그 사이 조회는 포기한다. */
  private static final int MAX_WINDOW_HOURS = 24;

  private static final String AGGREGATE_SQL =
      """
      SELECT alcohol_id, COUNT(*) AS viewer_count
      FROM alcohols_view_histories
      WHERE view_at >= ? AND view_at < ?
      GROUP BY alcohol_id
      """;

  private static final String PREVIOUS_SQL =
      """
      SELECT o.alcohol_id, o.bucket_at, o.cumulative_viewer_count
      FROM alcohol_interest_observations o
      JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
            FROM alcohol_interest_observations
            WHERE bucket_at < ?
            GROUP BY alcohol_id) latest
        ON o.alcohol_id = latest.alcohol_id AND o.bucket_at = latest.max_bucket
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_interest_observations
        (alcohol_id, bucket_at, observed_at, prev_bucket_at, viewer_count, cumulative_viewer_count)
      VALUES (?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        viewer_count = VALUES(viewer_count),
        cumulative_viewer_count = VALUES(cumulative_viewer_count)
      """;

  private final ObservationWriter writer;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    LocalDateTime bucketAt =
        ObservationBucket.from(
            chunkContext.getStepContext().getStepExecution().getJobParameters());
    LocalDateTime observedAt = LocalDateTime.now();

    LocalDateTime previousBucket = writer.findPreviousBucket(TABLE, bucketAt);
    LocalDateTime from = previousBucket != null ? previousBucket : bucketAt.minusHours(1);

    // 배치가 오래 멈췄다 재개하면 구간이 며칠로 벌어져 그동안의 조회가 한 버킷에 몰린다.
    // 흐름 축은 어차피 놓친 구간을 되살릴 수 없으니, 몰아 넣기보다 상한에서 끊는 편이 낫다.
    LocalDateTime floor = bucketAt.minusHours(MAX_WINDOW_HOURS);
    if (from.isBefore(floor)) {
      log.warn(
          "관심도 관측 구간이 상한을 넘어 잘라냅니다. from={}, floor={}, bucketAt={}",
          from,
          floor,
          bucketAt);
      from = floor;
    }

    Map<Long, Long> viewerCounts = new HashMap<>();
    writer
        .jdbc()
        .query(
            AGGREGATE_SQL,
            rs -> {
              viewerCounts.put(rs.getLong("alcohol_id"), rs.getLong("viewer_count"));
            },
            from,
            bucketAt);

    Map<Long, Long> previousCumulative = new HashMap<>();
    Map<Long, LocalDateTime> previousBucketByAlcohol = new HashMap<>();
    writer
        .jdbc()
        .query(
            PREVIOUS_SQL,
            rs -> {
              long alcoholId = rs.getLong("alcohol_id");
              previousCumulative.put(alcoholId, rs.getLong("cumulative_viewer_count"));
              previousBucketByAlcohol.put(
                  alcoholId, rs.getObject("bucket_at", LocalDateTime.class));
            },
            bucketAt);

    List<Object[]> rows = new ArrayList<>(viewerCounts.size());
    for (Map.Entry<Long, Long> entry : viewerCounts.entrySet()) {
      Long alcoholId = entry.getKey();
      Long viewerCount = entry.getValue();
      long cumulative = previousCumulative.getOrDefault(alcoholId, 0L) + viewerCount;
      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            previousBucketByAlcohol.get(alcoholId),
            viewerCount,
            cumulative
          });
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info(
        "관심도 관측 완료. bucketAt={}, 구간=[{}, {}), 적재={}행", bucketAt, from, bucketAt, rows.size());
    return RepeatStatus.FINISHED;
  }
}
