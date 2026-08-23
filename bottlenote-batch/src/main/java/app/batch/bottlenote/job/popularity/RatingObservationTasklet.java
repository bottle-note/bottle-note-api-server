package app.batch.bottlenote.job.popularity;

import java.math.BigDecimal;
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
 * 평가도 관측.
 *
 * <p>상태 축이다. 원본에 누적이 남아 있어 언제 세도 현재값이 나오므로, 배치를 놓쳐도 다음 회차가 저절로 정합된다.
 *
 * <p>0점은 애플리케이션에서 미평가(삭제)로 취급하므로 집계에서 뺀다. 이 필터가 없으면 조회 API가 보여주는 평균·표본과 배치 점수의 모집단이 달라진다.
 *
 * <p>평균이 아니라 합과 개수를 담는다. 평균만 담으면 나중에 버킷을 롤업할 때 합칠 수 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingObservationTasklet implements Tasklet {

  private static final String TABLE = "alcohol_rating_observations";

  private static final String AGGREGATE_SQL =
      """
      SELECT alcohol_id,
             COUNT(*) AS rating_count,
             COALESCE(SUM(rating), 0) AS rating_sum
      FROM ratings
      WHERE rating > 0.0
      GROUP BY alcohol_id
      """;

  private static final String PREVIOUS_SQL =
      """
      SELECT o.alcohol_id, o.bucket_at, o.rating_count, o.rating_sum
      FROM alcohol_rating_observations o
      JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
            FROM alcohol_rating_observations
            WHERE bucket_at < ?
            GROUP BY alcohol_id) latest
        ON o.alcohol_id = latest.alcohol_id AND o.bucket_at = latest.max_bucket
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_rating_observations
        (alcohol_id, bucket_at, observed_at, prev_bucket_at,
         rating_count, rating_sum, delta_rating_count, delta_rating_sum)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        observed_at = VALUES(observed_at),
        prev_bucket_at = VALUES(prev_bucket_at),
        rating_count = VALUES(rating_count),
        rating_sum = VALUES(rating_sum),
        delta_rating_count = VALUES(delta_rating_count),
        delta_rating_sum = VALUES(delta_rating_sum)
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
                  new Snapshot(rs.getLong("rating_count"), rs.getBigDecimal("rating_sum"), null));
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
                      rs.getLong("rating_count"),
                      rs.getBigDecimal("rating_sum"),
                      rs.getObject("bucket_at", LocalDateTime.class)));
            },
            bucketAt);

    Set<Long> alreadyWritten = writer.findAlcoholIdsAt("alcohol_rating_observations", bucketAt);

    List<Object[]> rows = new ArrayList<>();
    int skipped = 0;

    for (Map.Entry<Long, Snapshot> entry : current.entrySet()) {
      Long alcoholId = entry.getKey();
      Snapshot now = entry.getValue();
      Snapshot before = previous.get(alcoholId);

      // 상태 축은 값이 그대로면 행을 남기지 않는다 — 갭은 prev_bucket_at으로 따라간다
      // 이번 버킷 행이 이미 있으면 재실행이다. 값이 같아도 다시 써야 1회차의 잘못된 값이 정정된다.
      if (before != null && before.sameValueAs(now) && !alreadyWritten.contains(alcoholId)) {
        skipped++;
        continue;
      }

      BigDecimal beforeSum = before == null ? BigDecimal.ZERO : before.sum();
      long beforeCount = before == null ? 0L : before.count();

      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            before == null ? null : before.bucketAt(),
            now.count(),
            now.sum(),
            now.count() - beforeCount,
            now.sum().subtract(beforeSum)
          });
    }

    // 이번 집계에 없는데 직전 관측이 있던 주류는 0으로 떨어진 것이다.
    // 남기지 않으면 최종 적재가 사라진 값을 계속 끌어온다.
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
            alcoholId,
            bucketAt,
            observedAt,
            before.bucketAt(),
            0L,
            BigDecimal.ZERO,
            -before.count(),
            before.sum().negate()
          });
      zeroed++;
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info(
        "평가도 관측 완료. bucketAt={}, 적재={}행(0으로 떨어짐 {}행), 변화 없어 건너뜀={}행",
        bucketAt,
        rows.size(),
        zeroed,
        skipped);
    return RepeatStatus.FINISHED;
  }

  private record Snapshot(long count, BigDecimal sum, LocalDateTime bucketAt) {
    Snapshot {
      sum = sum == null ? BigDecimal.ZERO : sum;
    }

    boolean sameValueAs(Snapshot other) {
      return count == other.count && sum.compareTo(other.sum) == 0;
    }

    boolean isZero() {
      return count == 0L && sum.signum() == 0;
    }
  }
}
