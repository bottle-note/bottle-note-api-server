package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.domain.AlcoholViewCounter;
import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <p>흐름 축이다. Product API가 Redis 시간 Hash에 기록한 기간 조회수를 읽고, 같은 버킷 재실행에서도 절대값으로 덮어쓴다.
 *
 * <p>희소 저장 규칙이 다른 축과 다르다. 흐름 축은 값이 같아도 매 구간이 새로운 사건이라 "직전과 같으면 생략"을 적용하면 안 된다. 대신 조회가 없는 주류는 집계
 * 결과에 아예 나오지 않으므로 결과적으로 희소해진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterestObservationTasklet implements Tasklet {

  private static final String TABLE = "alcohol_interest_observations";

  private static final String PREVIOUS_SQL =
      """
      SELECT o.alcohol_id, o.bucket_at, o.cumulative_view_count
      FROM alcohol_interest_observations o
      JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
            FROM alcohol_interest_observations
            WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
            GROUP BY alcohol_id) latest
        ON o.alcohol_id = latest.alcohol_id
       AND o.bucket_granularity = 'HOUR'
       AND o.bucket_at = latest.max_bucket
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_interest_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         view_count, cumulative_view_count)
      VALUES (?, 'HOUR', ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        view_count = VALUES(view_count),
        cumulative_view_count = VALUES(cumulative_view_count)
      """;

  private final ObservationWriter writer;
  private final AlcoholViewCounter alcoholViewCounter;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    LocalDateTime bucketAt =
        ObservationBucket.from(
            chunkContext.getStepContext().getStepExecution().getJobParameters());
    LocalDateTime observedAt = LocalDateTime.now();
    Map<Long, Long> viewCounts = alcoholViewCounter.findCounts(bucketAt);

    Map<Long, Long> previousCumulative = new HashMap<>();
    Map<Long, LocalDateTime> previousBucketByAlcohol = new HashMap<>();
    writer
        .jdbc()
        .query(
            PREVIOUS_SQL,
            rs -> {
              long alcoholId = rs.getLong("alcohol_id");
              previousCumulative.put(alcoholId, rs.getLong("cumulative_view_count"));
              previousBucketByAlcohol.put(
                  alcoholId, rs.getObject("bucket_at", LocalDateTime.class));
            },
            bucketAt);

    Set<Long> alcoholIds = new HashSet<>(viewCounts.keySet());
    alcoholIds.addAll(writer.findAlcoholIdsAt(TABLE, BucketGranularity.HOUR, bucketAt));

    List<Object[]> rows = new ArrayList<>(alcoholIds.size());
    for (Long alcoholId : alcoholIds) {
      long viewCount = viewCounts.getOrDefault(alcoholId, 0L);
      long cumulative = previousCumulative.getOrDefault(alcoholId, 0L) + viewCount;
      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            previousBucketByAlcohol.get(alcoholId),
            viewCount,
            cumulative
          });
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info("관심도 관측 완료. bucketAt={}, granularity=HOUR, 적재={}행", bucketAt, rows.size());
    return RepeatStatus.FINISHED;
  }
}
