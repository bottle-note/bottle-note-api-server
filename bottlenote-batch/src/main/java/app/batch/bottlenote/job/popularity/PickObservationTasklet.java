package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
 * 선호도 관측.
 *
 * <p>상태 축이다. 픽 취소는 행 삭제가 아니라 status 전이이므로 PICK만 세야 한다. 상태를 무시하고 세면 철회한 사용자가 계속 선호로 집계된다.
 *
 * <p>철회 신호를 따로 보기 위해 UNPICK도 함께 센다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickObservationTasklet implements Tasklet {

  private static final String TABLE = "alcohol_pick_observations";

  private static final String AGGREGATE_SQL =
      """
      SELECT alcohol_id,
             SUM(CASE WHEN status = 'PICK' THEN 1 ELSE 0 END) AS pick_count,
             SUM(CASE WHEN status = 'UNPICK' THEN 1 ELSE 0 END) AS unpick_count
      FROM picks
      GROUP BY alcohol_id
      """;

  private static final String PREVIOUS_SQL =
      """
      SELECT o.alcohol_id, o.bucket_at, o.pick_count, o.unpick_count
      FROM alcohol_pick_observations o
      JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
            FROM alcohol_pick_observations
            WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
            GROUP BY alcohol_id) latest
        ON o.alcohol_id = latest.alcohol_id
       AND o.bucket_granularity = 'HOUR'
       AND o.bucket_at = latest.max_bucket
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_pick_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         pick_count, unpick_count, delta_pick_count)
      VALUES (?, 'HOUR', ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        pick_count = VALUES(pick_count),
        unpick_count = VALUES(unpick_count),
        delta_pick_count = VALUES(delta_pick_count)
      """;

  private final ObservationWriter writer;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    LocalDateTime bucketAt =
        ObservationBucket.from(
            chunkContext.getStepContext().getStepExecution().getJobParameters());
    LocalDateTime observedAt = LocalDateTime.now();

    Map<Long, Snapshot> current = new HashMap<>();
    writer
        .jdbc()
        .query(
            AGGREGATE_SQL,
            rs -> {
              current.put(
                  rs.getLong("alcohol_id"),
                  new Snapshot(rs.getLong("pick_count"), rs.getLong("unpick_count"), null));
            });

    Map<Long, Snapshot> previous = new HashMap<>();
    writer
        .jdbc()
        .query(
            PREVIOUS_SQL,
            rs -> {
              previous.put(
                  rs.getLong("alcohol_id"),
                  new Snapshot(
                      rs.getLong("pick_count"),
                      rs.getLong("unpick_count"),
                      rs.getObject("bucket_at", LocalDateTime.class)));
            },
            bucketAt);

    Set<Long> alreadyWritten =
        writer.findAlcoholIdsAt(TABLE, BucketGranularity.HOUR, bucketAt);

    List<Object[]> rows = new ArrayList<>();
    int skipped = 0;

    for (Map.Entry<Long, Snapshot> entry : current.entrySet()) {
      Long alcoholId = entry.getKey();
      Snapshot now = entry.getValue();
      Snapshot before = previous.get(alcoholId);

      // 이번 버킷 행이 이미 있으면 재실행이다. 값이 같아도 다시 써야 1회차의 잘못된 값이 정정된다.
      if (before != null && before.sameValueAs(now) && !alreadyWritten.contains(alcoholId)) {
        skipped++;
        continue;
      }

      long beforePick = before == null ? 0L : before.pickCount();
      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            before == null ? null : before.bucketAt(),
            now.pickCount(),
            now.unpickCount(),
            now.pickCount() - beforePick
          });
    }

    // 픽이 전부 취소되어 집계에서 사라진 주류도 0으로 남긴다
    int zeroed = 0;
    for (Map.Entry<Long, Snapshot> entry : previous.entrySet()) {
      Long alcoholId = entry.getKey();
      if (current.containsKey(alcoholId)) {
        continue;
      }
      Snapshot before = entry.getValue();
      if (before.isZero()) {
        continue;
      }
      rows.add(
          new Object[] {
            alcoholId, bucketAt, observedAt, before.bucketAt(), 0L, 0L, -before.pickCount()
          });
      zeroed++;
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info(
        "선호도 관측 완료. bucketAt={}, 적재={}행(0으로 떨어짐 {}행), 변화 없어 건너뜀={}행",
        bucketAt,
        rows.size(),
        zeroed,
        skipped);
    return RepeatStatus.FINISHED;
  }

  private record Snapshot(long pickCount, long unpickCount, LocalDateTime bucketAt) {
    boolean sameValueAs(Snapshot other) {
      return pickCount == other.pickCount && unpickCount == other.unpickCount;
    }

    boolean isZero() {
      return pickCount == 0L && unpickCount == 0L;
    }
  }
}
