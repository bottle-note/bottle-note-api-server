package app.batch.bottlenote.job.ranking;

import app.batch.bottlenote.BatchQuartzJob;
import app.bottlenote.review.constant.BestReviewChangeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 일간 베스트 리뷰 선정.
 *
 * <p>실행마다 DB를 새로 읽어 이번 선정 집합을 만들고, 현재 is_best 집합과의 차이만 갱신한다. 전체 초기화 단계가 없으므로 재실행이나 변동 없는 날에는 아무
 * 것도 바꾸지 않는다.
 *
 * <p>좋아요·댓글·이미지를 한 쿼리로 조인하면 카티전 곱으로 서로를 부풀린다. 그래서 지표별로 따로 집계해 메모리에서 합친다.
 *
 * <p>변동은 선정·해제 각각 로그로 남긴다. 같은 날 같은 리뷰의 같은 변동은 유니크 키로 덮어써 재실행이 행을 늘리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BestReviewSelectionTasklet implements Tasklet {

  private static final int BATCH_SIZE = 500;

  private static final String REVIEW_SQL =
      """
      SELECT id, alcohol_id, CHAR_LENGTH(content) AS content_length
      FROM reviews
      WHERE active_status = 'ACTIVE' AND status = 'PUBLIC'
      """;

  private static final String LIKE_SQL =
      """
      SELECT review_id,
             SUM(CASE WHEN status = 'LIKE' THEN 1 ELSE 0 END) AS like_count,
             SUM(CASE WHEN status = 'DISLIKE' THEN 1 ELSE 0 END) AS dislike_count
      FROM likes
      GROUP BY review_id
      """;

  /** 작성자 본인 댓글은 빼고, 같은 사람이 여러 번 달아도 한 명으로 센다. */
  private static final String REPLIER_SQL =
      """
      SELECT rr.review_id, COUNT(DISTINCT rr.user_id) AS replier_count
      FROM review_replies rr
      JOIN reviews r ON r.id = rr.review_id
      WHERE rr.status = 'NORMAL' AND rr.user_id <> r.user_id
      GROUP BY rr.review_id
      """;

  private static final String IMAGE_SQL =
      """
      SELECT review_id, COUNT(*) AS image_count
      FROM review_images
      GROUP BY review_id
      """;

  private static final String CURRENT_BEST_SQL =
      """
      SELECT id, alcohol_id
      FROM reviews
      WHERE is_best = TRUE
      """;

  private static final String INSERT_LOG_SQL =
      """
      INSERT INTO best_review_selection_logs
        (selection_date, review_id, alcohol_id, change_type, ranking, score,
         like_count, dislike_count, reply_count, image_count, content_length,
         alcohol_review_count, rule_code, reason, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        alcohol_id = VALUES(alcohol_id),
        ranking = VALUES(ranking),
        score = VALUES(score),
        like_count = VALUES(like_count),
        dislike_count = VALUES(dislike_count),
        reply_count = VALUES(reply_count),
        image_count = VALUES(image_count),
        content_length = VALUES(content_length),
        alcohol_review_count = VALUES(alcohol_review_count),
        rule_code = VALUES(rule_code),
        reason = VALUES(reason),
        created_at = VALUES(created_at)
      """;

  private final JdbcTemplate jdbcTemplate;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    JobParameters parameters =
        chunkContext.getStepContext().getStepExecution().getJobParameters();
    LocalDateTime executedAt = executionTime(parameters);
    LocalDate selectionDate = executedAt.toLocalDate();

    Map<Long, BestReviewCandidate> candidates = loadCandidates();
    Map<Long, Long> currentBest = loadCurrentBest();

    List<Object[]> selectedRows = new ArrayList<>();
    List<Object[]> releasedRows = new ArrayList<>();

    for (BestReviewCandidate candidate : candidates.values()) {
      if (candidate.isSelected() && !currentBest.containsKey(candidate.reviewId())) {
        selectedRows.add(
            logRow(selectionDate, executedAt, candidate, BestReviewChangeType.SELECTED));
      }
    }

    for (Map.Entry<Long, Long> entry : currentBest.entrySet()) {
      long reviewId = entry.getKey();
      BestReviewCandidate candidate = candidates.get(reviewId);
      if (candidate != null && candidate.isSelected()) {
        continue;
      }
      if (candidate == null) {
        releasedRows.add(
            releasedRow(selectionDate, executedAt, reviewId, entry.getValue()));
      } else {
        releasedRows.add(
            logRow(selectionDate, executedAt, candidate, BestReviewChangeType.RELEASED));
      }
    }

    // 해제를 먼저 적재해 같은 날 로그를 시간순으로 읽었을 때 빠진 리뷰가 먼저 보이게 한다.
    updateBestFlag(releasedRows, false);
    updateBestFlag(selectedRows, true);
    batchInsert(INSERT_LOG_SQL, releasedRows);
    batchInsert(INSERT_LOG_SQL, selectedRows);

    contribution.incrementWriteCount(selectedRows.size() + releasedRows.size());
    log.info(
        "베스트 리뷰 선정 완료. selectionDate={}, 후보={}건, 현재 베스트={}건, 선정={}건, 해제={}건",
        selectionDate,
        candidates.size(),
        currentBest.size(),
        selectedRows.size(),
        releasedRows.size());
    return RepeatStatus.FINISHED;
  }

  private static LocalDateTime executionTime(JobParameters parameters) {
    LocalDateTime executedAt =
        parameters == null ? null : parameters.getLocalDateTime(BatchQuartzJob.EXECUTION_TIME_PARAM);
    return executedAt == null ? LocalDateTime.now() : executedAt;
  }

  /** ACTIVE·PUBLIC 리뷰만 후보다. 지표는 리뷰별로 따로 집계해 곱해지지 않게 한다. */
  private Map<Long, BestReviewCandidate> loadCandidates() {
    Map<Long, long[]> reviews = new LinkedHashMap<>();
    // [0]=alcohol_id, [1]=content_length
    jdbcTemplate.query(
        REVIEW_SQL,
        rs -> {
          reviews.put(
              rs.getLong("id"), new long[] {rs.getLong("alcohol_id"), rs.getLong("content_length")});
        });

    Map<Long, long[]> likes = new HashMap<>();
    jdbcTemplate.query(
        LIKE_SQL,
        rs -> {
          likes.put(
              rs.getLong("review_id"),
              new long[] {rs.getLong("like_count"), rs.getLong("dislike_count")});
        });
    Map<Long, Long> repliers = new HashMap<>();
    jdbcTemplate.query(
        REPLIER_SQL,
        rs -> {
          repliers.put(rs.getLong("review_id"), rs.getLong("replier_count"));
        });
    Map<Long, Long> images = new HashMap<>();
    jdbcTemplate.query(
        IMAGE_SQL,
        rs -> {
          images.put(rs.getLong("review_id"), rs.getLong("image_count"));
        });

    Map<Long, Long> reviewCountByAlcohol = new HashMap<>();
    for (long[] review : reviews.values()) {
      reviewCountByAlcohol.merge(review[0], 1L, Long::sum);
    }

    Map<Long, List<BestReviewCandidate>> byAlcohol = new HashMap<>();
    for (Map.Entry<Long, long[]> entry : reviews.entrySet()) {
      long reviewId = entry.getKey();
      long alcoholId = entry.getValue()[0];
      long contentLength = entry.getValue()[1];
      long[] likeCounts = likes.getOrDefault(reviewId, new long[2]);
      long replierCount = repliers.getOrDefault(reviewId, 0L);
      long imageCount = images.getOrDefault(reviewId, 0L);
      BigDecimal score =
          BestReviewSelectionRule.score(likeCounts[0], likeCounts[1], replierCount, imageCount);
      byAlcohol
          .computeIfAbsent(alcoholId, key -> new ArrayList<>())
          .add(
              new BestReviewCandidate(
                  reviewId,
                  alcoholId,
                  likeCounts[0],
                  likeCounts[1],
                  replierCount,
                  imageCount,
                  contentLength,
                  reviewCountByAlcohol.get(alcoholId),
                  score,
                  0));
    }

    // 동점이면 최신 리뷰가 앞선다. 오래된 리뷰가 베스트에 고착되는 것을 막는다.
    Comparator<BestReviewCandidate> order =
        Comparator.comparing(BestReviewCandidate::score)
            .reversed()
            .thenComparing(Comparator.comparingLong(BestReviewCandidate::reviewId).reversed());

    Map<Long, BestReviewCandidate> ranked = new LinkedHashMap<>();
    for (List<BestReviewCandidate> group : byAlcohol.values()) {
      group.sort(order);
      for (int index = 0; index < group.size(); index++) {
        BestReviewCandidate candidate = group.get(index);
        ranked.put(candidate.reviewId(), candidate.withRanking(index + 1));
      }
    }
    return ranked;
  }

  private Map<Long, Long> loadCurrentBest() {
    Map<Long, Long> current = new LinkedHashMap<>();
    jdbcTemplate.query(
        CURRENT_BEST_SQL, rs -> {
          current.put(rs.getLong("id"), rs.getLong("alcohol_id"));
        });
    return current;
  }

  private static Object[] logRow(
      LocalDate selectionDate,
      LocalDateTime executedAt,
      BestReviewCandidate candidate,
      BestReviewChangeType changeType) {
    return new Object[] {
      selectionDate,
      candidate.reviewId(),
      candidate.alcoholId(),
      changeType.name(),
      candidate.ranking(),
      candidate.score(),
      candidate.likeCount(),
      candidate.dislikeCount(),
      candidate.replierCount(),
      candidate.imageCount(),
      candidate.contentLength(),
      candidate.alcoholReviewCount(),
      ruleCodeOf(candidate, changeType),
      reasonOf(candidate, changeType),
      executedAt
    };
  }

  /** 삭제되거나 비공개로 바뀐 리뷰는 후보 목록에 없어 수치를 알 수 없다. 0으로 남기고 사유만 적는다. */
  private static Object[] releasedRow(
      LocalDate selectionDate, LocalDateTime executedAt, long reviewId, long alcoholId) {
    return new Object[] {
      selectionDate,
      reviewId,
      alcoholId,
      BestReviewChangeType.RELEASED.name(),
      null,
      BigDecimal.ZERO.setScale(BestReviewSelectionRule.SCALE),
      0L,
      0L,
      0L,
      0L,
      0L,
      0L,
      BestReviewSelectionRule.RULE_NOT_ACTIVE_PUBLIC,
      "리뷰가 삭제되었거나 비공개로 바뀌어 후보에서 제외되었습니다.",
      executedAt
    };
  }

  private static String ruleCodeOf(BestReviewCandidate candidate, BestReviewChangeType changeType) {
    if (changeType == BestReviewChangeType.RELEASED && !candidate.isEligible()) {
      return BestReviewSelectionRule.RULE_ELIGIBILITY_NOT_MET;
    }
    return candidate.tierRuleCode();
  }

  private static String reasonOf(BestReviewCandidate candidate, BestReviewChangeType changeType) {
    String metrics =
        "좋아요 %d, 싫어요 %d, 댓글 작성자 %d명, 이미지 %d, 본문 %d자, 점수 %s"
            .formatted(
                candidate.likeCount(),
                candidate.dislikeCount(),
                candidate.replierCount(),
                candidate.imageCount(),
                candidate.contentLength(),
                candidate.score().toPlainString());
    int allowed = BestReviewSelectionRule.allowedCount(candidate.alcoholReviewCount());
    if (changeType == BestReviewChangeType.SELECTED) {
      return "주류 리뷰 %d건 중 %d위로 상위 %d건 안에 들어 선정되었습니다. %s"
          .formatted(candidate.alcoholReviewCount(), candidate.ranking(), allowed, metrics);
    }
    if (!candidate.isEligible()) {
      return "%s 기준에 미달해 해제되었습니다. %s".formatted(unmetConditions(candidate), metrics);
    }
    return "주류 리뷰 %d건 중 %d위로 상위 %d건 밖으로 밀려 해제되었습니다. %s"
        .formatted(candidate.alcoholReviewCount(), candidate.ranking(), allowed, metrics);
  }

  private static String unmetConditions(BestReviewCandidate candidate) {
    List<String> unmet = new ArrayList<>();
    if (candidate.alcoholReviewCount() < BestReviewSelectionRule.MIN_ALCOHOL_REVIEWS) {
      unmet.add(
          "주류 리뷰 %d건(최소 %d건)"
              .formatted(candidate.alcoholReviewCount(), BestReviewSelectionRule.MIN_ALCOHOL_REVIEWS));
    }
    if (candidate.netLikes() < BestReviewSelectionRule.MIN_NET_LIKES) {
      unmet.add(
          "순좋아요 %d(최소 %d)".formatted(candidate.netLikes(), BestReviewSelectionRule.MIN_NET_LIKES));
    }
    if (candidate.contentLength() < BestReviewSelectionRule.MIN_CONTENT_LENGTH) {
      unmet.add(
          "본문 %d자(최소 %d자)"
              .formatted(candidate.contentLength(), BestReviewSelectionRule.MIN_CONTENT_LENGTH));
    }
    return String.join(", ", unmet);
  }

  private void updateBestFlag(List<Object[]> rows, boolean best) {
    if (rows.isEmpty()) {
      return;
    }
    List<Object[]> ids = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      ids.add(new Object[] {best, row[1]});
    }
    batchInsert("UPDATE reviews SET is_best = ? WHERE id = ?", ids);
  }

  private void batchInsert(String sql, List<Object[]> rows) {
    if (rows.isEmpty()) {
      return;
    }
    for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, rows.size());
      jdbcTemplate.batchUpdate(sql, rows.subList(from, to));
    }
  }
}
