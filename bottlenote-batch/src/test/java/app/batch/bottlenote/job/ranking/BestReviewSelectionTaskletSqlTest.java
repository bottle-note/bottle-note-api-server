package app.batch.bottlenote.job.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import app.batch.bottlenote.BatchQuartzJob;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 선정 SQL과 변동 로그를 실제 DB에 실행해 검증한다.
 *
 * <p>옛 구현은 리더가 첫 실행 결과를 캐시해 둘째 날부터 아무것도 선정하지 않았다. 같은 Tasklet 인스턴스로 여러 날을 돌려 그 회귀를 막는다.
 */
@Tag("batch")
@DisplayName("[batch] 베스트 리뷰 선정 실행")
class BestReviewSelectionTaskletSqlTest {

  private static final LocalDateTime DAY_1 = LocalDateTime.of(2026, 9, 23, 0, 0);
  private static final LocalDateTime DAY_2 = DAY_1.plusDays(1);
  private static final long AUTHOR = 1L;
  private static final String LONG_CONTENT = "스뱅 특유의 쿰쿰함이 노즈와 팔레트를 지나 피니시로 이어질 때의 만족감이 상당하다.";
  private static final String SHORT_CONTENT = "맛있다";

  private JdbcTemplate jdbc;
  private BestReviewSelectionTasklet tasklet;

  @BeforeEach
  void setUp() {
    jdbc = BestReviewSqlSupport.freshDatabase();
    tasklet = new BestReviewSelectionTasklet(jdbc);
  }

  @Test
  @DisplayName("주류 리뷰 2건 이상이면 점수 1위를 선정하고 SELECTED 로그를 남긴다")
  void selectsTopReviewAndLogsReason() throws Exception {
    review(1, 100);
    review(2, 100);
    review(3, 100);
    likes(2, 3, 0);
    likes(3, 1, 0);
    replies(3, 2);
    image(3, 1);

    run(DAY_1);

    assertThat(bestIds()).containsExactly(2L);
    List<Map<String, Object>> logs = logs();
    assertThat(logs).hasSize(1);
    Map<String, Object> log = logs.get(0);
    assertThat(log.get("review_id")).isEqualTo(2L);
    assertThat(log.get("change_type")).isEqualTo("SELECTED");
    assertThat(log.get("ranking")).isEqualTo(1);
    assertThat((BigDecimal) log.get("score")).isEqualByComparingTo("1.80");
    assertThat(log.get("alcohol_review_count")).isEqualTo(3L);
    assertThat(log.get("content_length")).isEqualTo(LONG_CONTENT.length());
    assertThat(log.get("rule_code")).isEqualTo(BestReviewSelectionRule.RULE_UNDER_10_TOP_1);
    assertThat(log.get("reason").toString()).contains("3건 중 1위", "좋아요 3");
    assertThat(log.get("selection_date").toString()).isEqualTo("2026-09-23");
  }

  @Test
  @DisplayName("좋아요와 이미지를 따로 세고 댓글은 작성자 본인을 뺀 사람 수로 센다")
  void countsMetricsWithoutCartesianProduct() throws Exception {
    review(1, 100);
    review(2, 100);
    likes(1, 3, 1);
    image(1, 3);
    replies(1, 2);
    jdbc.update(
        "INSERT INTO review_replies (review_id, user_id, status) VALUES (1, ?, 'NORMAL')", AUTHOR);
    // 같은 사람이 두 번 달아도 한 명이다
    jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (1, 200, 'NORMAL')");

    run(DAY_1);

    Map<String, Object> log = logs().get(0);
    assertThat(log.get("like_count")).isEqualTo(3L);
    assertThat(log.get("dislike_count")).isEqualTo(1L);
    assertThat(log.get("reply_count")).isEqualTo(2L);
    assertThat(log.get("image_count")).isEqualTo(3L);
    assertThat((BigDecimal) log.get("score")).isEqualByComparingTo("1.80");
  }

  @Test
  @DisplayName("리뷰가 한 건뿐인 주류는 좋아요가 많아도 선정하지 않는다")
  void skipsLonelyReview() throws Exception {
    review(1, 100);
    likes(1, 9, 0);

    run(DAY_1);

    assertThat(bestIds()).isEmpty();
    assertThat(logs()).isEmpty();
  }

  @Test
  @DisplayName("순좋아요가 없거나 본문이 짧은 리뷰는 후보에서 뺀다")
  void skipsReviewsBelowQualityFloor() throws Exception {
    review(1, 100);
    review(2, 100);
    review(3, 100, SHORT_CONTENT);
    likes(1, 2, 2);
    likes(3, 5, 0);

    run(DAY_1);

    assertThat(bestIds()).isEmpty();
  }

  @Test
  @DisplayName("삭제·비공개 리뷰는 후보에서 빼고 주류 리뷰 수에도 세지 않는다")
  void ignoresInactiveOrPrivateReviews() throws Exception {
    review(1, 100);
    likes(1, 3, 0);
    jdbc.update(
        "INSERT INTO reviews (id, alcohol_id, user_id, content, status, active_status) VALUES (3, 100, 1, ?, 'PUBLIC', 'DELETED')",
        LONG_CONTENT);
    jdbc.update(
        "INSERT INTO reviews (id, alcohol_id, user_id, content, status, active_status) VALUES (4, 100, 1, ?, 'PRIVATE', 'ACTIVE')",
        LONG_CONTENT);
    likes(3, 9, 0);

    run(DAY_1);

    assertThat(bestIds()).isEmpty();
  }

  @Test
  @DisplayName("동점이면 최신 리뷰가 이긴다")
  void tieGoesToNewestReview() throws Exception {
    review(1, 100);
    review(2, 100);
    likes(1, 2, 0);
    likes(2, 2, 0);

    run(DAY_1);

    assertThat(bestIds()).containsExactly(2L);
  }

  @Test
  @DisplayName("같은 날 다시 실행해도 변동이 없으면 로그와 상태를 건드리지 않는다")
  void rerunWithoutChangeIsIdempotent() throws Exception {
    review(1, 100);
    review(2, 100);
    likes(1, 2, 0);

    run(DAY_1);
    run(DAY_1);
    run(DAY_2);

    assertThat(bestIds()).containsExactly(1L);
    assertThat(logs()).hasSize(1);
  }

  @Test
  @DisplayName("다음 날 순위가 바뀌면 해제와 선정을 각각 기록한다")
  void logsReleaseAndSelectionWhenRankingChanges() throws Exception {
    review(1, 100);
    review(2, 100);
    likes(1, 2, 0);
    run(DAY_1);

    likes(2, 5, 0);
    run(DAY_2);

    assertThat(bestIds()).containsExactly(2L);
    List<Map<String, Object>> logs = logs();
    assertThat(logs).hasSize(3);
    Map<String, Object> released = logs.get(1);
    assertThat(released.get("review_id")).isEqualTo(1L);
    assertThat(released.get("change_type")).isEqualTo("RELEASED");
    assertThat(released.get("ranking")).isEqualTo(2);
    assertThat(released.get("reason").toString()).contains("2위", "상위 1건 밖");
    Map<String, Object> selected = logs.get(2);
    assertThat(selected.get("review_id")).isEqualTo(2L);
    assertThat(selected.get("change_type")).isEqualTo("SELECTED");
  }

  @Test
  @DisplayName("베스트 리뷰가 삭제되면 후보 없음 사유로 해제한다")
  void releasesDeletedBestReview() throws Exception {
    review(1, 100);
    review(2, 100);
    review(3, 100);
    likes(1, 2, 0);
    likes(2, 1, 0);
    run(DAY_1);

    jdbc.update("UPDATE reviews SET active_status = 'DELETED' WHERE id = 1");
    run(DAY_2);

    assertThat(bestIds()).containsExactly(2L);
    Map<String, Object> released = logs().get(1);
    assertThat(released.get("review_id")).isEqualTo(1L);
    assertThat(released.get("change_type")).isEqualTo("RELEASED");
    assertThat(released.get("ranking")).isNull();
    assertThat(released.get("rule_code")).isEqualTo(BestReviewSelectionRule.RULE_NOT_ACTIVE_PUBLIC);
  }

  @Test
  @DisplayName("싫어요가 늘어 순좋아요가 사라지면 기준 미달 사유로 해제한다")
  void releasesWhenEligibilityIsLost() throws Exception {
    review(1, 100);
    review(2, 100);
    likes(1, 2, 0);
    run(DAY_1);

    likes(1, 0, 2);
    run(DAY_2);

    assertThat(bestIds()).isEmpty();
    Map<String, Object> released = logs().get(1);
    assertThat(released.get("rule_code")).isEqualTo(BestReviewSelectionRule.RULE_ELIGIBILITY_NOT_MET);
    assertThat(released.get("reason").toString()).contains("순좋아요 0", "미달");
  }

  @Test
  @DisplayName("리뷰 10건 이상이면 2건, 20건 이상이면 3건을 선정한다")
  void selectsByTier() throws Exception {
    for (int id = 1; id <= 10; id++) {
      review(id, 100);
      likes(id, id, 0);
    }
    for (int id = 11; id <= 30; id++) {
      review(id, 200);
      likes(id, id, 0);
    }

    run(DAY_1);

    assertThat(bestIds()).containsExactly(9L, 10L, 28L, 29L, 30L);
    assertThat(logs())
        .extracting(row -> row.get("rule_code"))
        .containsOnly(
            BestReviewSelectionRule.RULE_10_TO_19_TOP_2, BestReviewSelectionRule.RULE_20_OVER_TOP_3);
  }

  private void run(LocalDateTime executedAt) throws Exception {
    JobParameters parameters =
        new JobParametersBuilder()
            .addLocalDateTime(BatchQuartzJob.EXECUTION_TIME_PARAM, executedAt)
            .toJobParameters();
    JobExecution jobExecution =
        MetaDataInstanceFactory.createJobExecution("bestReviewSelectedJob", 1L, 1L, parameters);
    StepExecution stepExecution = jobExecution.createStepExecution("step");
    ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
    tasklet.execute(stepExecution.createStepContribution(), chunkContext);
  }

  private void review(long id, long alcoholId) {
    review(id, alcoholId, LONG_CONTENT);
  }

  private void review(long id, long alcoholId, String content) {
    jdbc.update(
        "INSERT INTO reviews (id, alcohol_id, user_id, content, status, active_status) VALUES (?, ?, ?, ?, 'PUBLIC', 'ACTIVE')",
        id,
        alcoholId,
        AUTHOR,
        content);
  }

  private void likes(long reviewId, int likeCount, int dislikeCount) {
    for (int i = 0; i < likeCount; i++) {
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (?, ?, 'LIKE')", reviewId, 10 + i);
    }
    for (int i = 0; i < dislikeCount; i++) {
      jdbc.update(
          "INSERT INTO likes (review_id, user_id, status) VALUES (?, ?, 'DISLIKE')", reviewId, 100 + i);
    }
  }

  /** 서로 다른 타인이 하나씩 단 NORMAL 댓글과 삭제된 댓글 하나. */
  private void replies(long reviewId, int repliers) {
    for (int i = 0; i < repliers; i++) {
      jdbc.update(
          "INSERT INTO review_replies (review_id, user_id, status) VALUES (?, ?, 'NORMAL')",
          reviewId,
          200 + i);
    }
    jdbc.update(
        "INSERT INTO review_replies (review_id, user_id, status) VALUES (?, 300, 'DELETED')", reviewId);
  }

  private void image(long reviewId, int count) {
    for (int i = 0; i < count; i++) {
      jdbc.update("INSERT INTO review_images (review_id) VALUES (?)", reviewId);
    }
  }

  private List<Long> bestIds() {
    return jdbc.queryForList("SELECT id FROM reviews WHERE is_best = TRUE ORDER BY id", Long.class);
  }

  private List<Map<String, Object>> logs() {
    return jdbc.queryForList("SELECT * FROM best_review_selection_logs ORDER BY id");
  }
}
