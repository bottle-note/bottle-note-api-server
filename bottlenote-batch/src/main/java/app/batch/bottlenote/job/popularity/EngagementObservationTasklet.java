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
 * 참여도 관측.
 *
 * <p>상태 축이다. 리뷰 자체와 리뷰에 붙는 2차 반응(좋아요·댓글)을 함께 센다.
 *
 * <p>세 지표를 한 쿼리로 조인하면 카티전 곱이 생겨 좋아요와 댓글이 서로를 부풀린다. 그래서 쿼리를 셋으로 나눠 각각 집계하고 메모리에서 합친다.
 *
 * <p>리뷰는 ACTIVE인 것만이 아니라 PUBLIC인 것만 센다 — 비공개 리뷰는 남에게 보이지 않으므로 참여로 집계하면 안 된다. 좋아요는 LIKE와 DISLIKE를
 * 분리한다. 취소가 상태 전이라서 합쳐 세면 철회가 참여로 잡힌다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementObservationTasklet implements Tasklet {

  private static final String TABLE = "alcohol_engagement_observations";

  private static final String REVIEW_SQL =
      """
      SELECT alcohol_id, COUNT(*) AS review_count
      FROM reviews
      WHERE active_status = 'ACTIVE' AND status = 'PUBLIC'
      GROUP BY alcohol_id
      """;

  private static final String LIKE_SQL =
      """
      SELECT r.alcohol_id,
             SUM(CASE WHEN l.status = 'LIKE' THEN 1 ELSE 0 END) AS like_count,
             SUM(CASE WHEN l.status = 'DISLIKE' THEN 1 ELSE 0 END) AS dislike_count
      FROM likes l
      JOIN reviews r ON r.id = l.review_id
      WHERE r.active_status = 'ACTIVE' AND r.status = 'PUBLIC'
      GROUP BY r.alcohol_id
      """;

  private static final String REPLY_SQL =
      """
      SELECT r.alcohol_id, COUNT(*) AS reply_count
      FROM review_replies rr
      JOIN reviews r ON r.id = rr.review_id
      WHERE rr.status = 'NORMAL' AND r.active_status = 'ACTIVE' AND r.status = 'PUBLIC'
      GROUP BY r.alcohol_id
      """;

  private static final String PREVIOUS_SQL =
      """
      SELECT o.alcohol_id, o.bucket_at, o.review_count, o.like_count, o.dislike_count, o.reply_count
      FROM alcohol_engagement_observations o
      JOIN (SELECT alcohol_id, MAX(bucket_at) AS max_bucket
            FROM alcohol_engagement_observations
            WHERE bucket_granularity = 'HOUR' AND bucket_at < ?
            GROUP BY alcohol_id) latest
        ON o.alcohol_id = latest.alcohol_id
       AND o.bucket_granularity = 'HOUR'
       AND o.bucket_at = latest.max_bucket
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO alcohol_engagement_observations
        (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
         review_count, like_count, dislike_count, reply_count,
         delta_review_count, delta_like_count, delta_dislike_count, delta_reply_count)
      VALUES (?, 'HOUR', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
    LocalDateTime bucketAt =
        ObservationBucket.from(
            chunkContext.getStepContext().getStepExecution().getJobParameters());
    LocalDateTime observedAt = LocalDateTime.now();

    Map<Long, long[]> current = new HashMap<>();
    // [0]=review, [1]=like, [2]=dislike, [3]=reply
    writer
        .jdbc()
        .query(
            REVIEW_SQL,
            rs -> {
              slot(current, rs.getLong("alcohol_id"))[0] = rs.getLong("review_count");
            });
    writer
        .jdbc()
        .query(
            LIKE_SQL,
            rs -> {
              long[] counts = slot(current, rs.getLong("alcohol_id"));
              counts[1] = rs.getLong("like_count");
              counts[2] = rs.getLong("dislike_count");
            });
    writer
        .jdbc()
        .query(
            REPLY_SQL,
            rs -> {
              slot(current, rs.getLong("alcohol_id"))[3] = rs.getLong("reply_count");
            });

    Map<Long, long[]> previous = new HashMap<>();
    Map<Long, LocalDateTime> previousBucket = new HashMap<>();
    writer
        .jdbc()
        .query(
            PREVIOUS_SQL,
            rs -> {
              long alcoholId = rs.getLong("alcohol_id");
              previous.put(
                  alcoholId,
                  new long[] {
                    rs.getLong("review_count"),
                    rs.getLong("like_count"),
                    rs.getLong("dislike_count"),
                    rs.getLong("reply_count")
                  });
              previousBucket.put(alcoholId, rs.getObject("bucket_at", LocalDateTime.class));
            },
            bucketAt);

    Set<Long> alreadyWritten =
        writer.findAlcoholIdsAt(TABLE, BucketGranularity.HOUR, bucketAt);

    List<Object[]> rows = new ArrayList<>();
    int skipped = 0;

    for (Map.Entry<Long, long[]> entry : current.entrySet()) {
      Long alcoholId = entry.getKey();
      long[] now = entry.getValue();
      long[] before = previous.get(alcoholId);

      // 이번 버킷 행이 이미 있으면 재실행이다. 값이 같아도 다시 써야 1회차의 잘못된 값이 정정된다.
      if (before != null && sameValues(before, now) && !alreadyWritten.contains(alcoholId)) {
        skipped++;
        continue;
      }

      long[] base = before == null ? new long[4] : before;
      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            previousBucket.get(alcoholId),
            now[0], now[1], now[2], now[3],
            now[0] - base[0], now[1] - base[1], now[2] - base[2], now[3] - base[3]
          });
    }

    // 리뷰가 전부 삭제·비공개로 바뀌어 집계에서 사라진 주류도 0으로 남긴다
    int zeroed = 0;
    for (Map.Entry<Long, long[]> entry : previous.entrySet()) {
      Long alcoholId = entry.getKey();
      if (current.containsKey(alcoholId)) {
        continue;
      }
      long[] before = entry.getValue();
      if (isAllZero(before)) {
        continue;
      }
      rows.add(
          new Object[] {
            alcoholId,
            bucketAt,
            observedAt,
            previousBucket.get(alcoholId),
            0L, 0L, 0L, 0L,
            -before[0], -before[1], -before[2], -before[3]
          });
      zeroed++;
    }

    writer.batchInsert(INSERT_SQL, rows);
    contribution.incrementWriteCount(rows.size());
    log.info(
        "참여도 관측 완료. bucketAt={}, 적재={}행(0으로 떨어짐 {}행), 변화 없어 건너뜀={}행",
        bucketAt,
        rows.size(),
        zeroed,
        skipped);
    return RepeatStatus.FINISHED;
  }

  private static long[] slot(Map<Long, long[]> target, long alcoholId) {
    return target.computeIfAbsent(alcoholId, key -> new long[4]);
  }

  private static boolean isAllZero(long[] counts) {
    for (long value : counts) {
      if (value != 0L) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameValues(long[] before, long[] now) {
    for (int i = 0; i < 4; i++) {
      if (before[i] != now[i]) {
        return false;
      }
    }
    return true;
  }
}
