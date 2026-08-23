package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholViewCounter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 관측 SQL을 실제 DB에 실행해 검증한다.
 *
 * <p>SQL은 문자열이라 컴파일러가 봐주지 않는다. 컬럼 오타나 조인 실수는 실행해 봐야만 드러난다.
 */
@Tag("batch")
@DisplayName("[batch] 관측 SQL 실행")
class ObservationTaskletSqlTest {

  private static final LocalDateTime BUCKET = LocalDateTime.of(2026, 8, 23, 12, 0);
  private static final LocalDateTime PREV_BUCKET = BUCKET.minusHours(1);

  private JdbcTemplate jdbc;
  private ObservationWriter writer;
  private InMemoryAlcoholViewCounter alcoholViewCounter;

  @BeforeEach
  void setUp() {
    jdbc = ObservationSqlSupport.freshDatabase();
    writer = new ObservationWriter(jdbc);
    alcoholViewCounter = new InMemoryAlcoholViewCounter();
    jdbc.update("INSERT INTO alcohols (id, deleted_at) VALUES (1, NULL), (2, NULL), (3, NULL)");
  }

  private void run(Tasklet tasklet, LocalDateTime bucket) throws Exception {
    JobParameters parameters =
        new JobParametersBuilder()
            .addLocalDateTime(ObservationBucket.EXECUTION_TIME_PARAM, bucket)
            .toJobParameters();
    JobExecution jobExecution =
        MetaDataInstanceFactory.createJobExecution("popularityObservationJob", 1L, 1L, parameters);
    StepExecution stepExecution = jobExecution.createStepExecution("step");
    ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
    tasklet.execute(stepExecution.createStepContribution(), chunkContext);
  }

  private void run(Tasklet tasklet, BucketGranularity granularity, LocalDateTime bucket)
      throws Exception {
    JobParameters parameters =
        new JobParametersBuilder()
            .addString(PopularityRollupTasklet.GRANULARITY_PARAM, granularity.name())
            .addLocalDateTime(ObservationBucket.BUCKET_AT_PARAM, bucket)
            .toJobParameters();
    JobExecution jobExecution =
        MetaDataInstanceFactory.createJobExecution("popularityRollupJob", 1L, 1L, parameters);
    StepExecution stepExecution = jobExecution.createStepExecution("step");
    ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
    tasklet.execute(stepExecution.createStepContribution(), chunkContext);
  }

  private List<Map<String, Object>> rowsOf(String table) {
    return jdbc.queryForList("SELECT * FROM " + table + " ORDER BY alcohol_id, bucket_at");
  }

  private List<Map<String, Object>> rowsOf(String table, String granularity) {
    return jdbc.queryForList(
        "SELECT * FROM "
            + table
            + " WHERE bucket_granularity = ? ORDER BY alcohol_id, bucket_at",
        granularity);
  }

  private InterestObservationTasklet interestTasklet() {
    return new InterestObservationTasklet(writer, alcoholViewCounter);
  }

  @Nested
  @DisplayName("관심도")
  class Interest {

    @Test
    @DisplayName("요청한 시간 Redis Hash의 조회수를 HOUR 행으로 저장한다")
    void writesCountsFromRequestedHourHash() throws Exception {
      alcoholViewCounter.put(PREV_BUCKET, Map.of(1L, 99L));
      alcoholViewCounter.put(BUCKET, Map.of(1L, 2L, 2L, 1L));

      run(interestTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows).allSatisfy(row -> assertThat(row.get("bucket_granularity")).isEqualTo("HOUR"));
      assertThat(rows.get(0).get("view_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("cumulative_view_count")).isEqualTo(2L);
      assertThat(alcoholViewCounter.requestedBuckets).containsExactly(BUCKET);
    }

    @Test
    @DisplayName("조회가 없는 주류는 행을 남기지 않는다")
    void writesNothingForUnviewedAlcohol() throws Exception {
      run(interestTasklet(), BUCKET);

      assertThat(rowsOf("alcohol_interest_observations")).isEmpty();
    }

    @Test
    @DisplayName("같은 버킷 재실행은 Redis 절대값으로 기존 행을 정정한다")
    void rerunCorrectsWithAbsoluteCount() throws Exception {
      alcoholViewCounter.put(BUCKET, Map.of(1L, 5L));
      run(interestTasklet(), BUCKET);

      alcoholViewCounter.put(BUCKET, Map.of(1L, 2L));
      run(interestTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("view_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("cumulative_view_count"))
          .as("첫 실행의 5에 다시 2를 더하지 않고 절대값 2로 정정한다")
          .isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 버킷 재실행에서 Redis 필드가 사라지면 기존 행을 0으로 정정한다")
    void rerunCorrectsMissingRedisFieldToZero() throws Exception {
      alcoholViewCounter.put(BUCKET, Map.of(1L, 5L));
      run(interestTasklet(), BUCKET);

      alcoholViewCounter.put(BUCKET, Map.of());
      run(interestTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("view_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("cumulative_view_count")).isEqualTo(0L);
    }

    @Test
    @DisplayName("누적은 직전 관측 누적에 이번 구간을 더한 값이다")
    void cumulativeAddsOnTopOfPrevious() throws Exception {
      alcoholViewCounter.put(PREV_BUCKET, Map.of(1L, 3L));
      run(interestTasklet(), PREV_BUCKET);
      alcoholViewCounter.put(BUCKET, Map.of(1L, 2L));
      run(interestTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("view_count")).isEqualTo(2L);
      assertThat(rows.get(1).get("cumulative_view_count")).isEqualTo(5L);
      assertThat(rows.get(1).get("prev_bucket_at")).isNotNull();
    }

    @Test
    @DisplayName("누적과 재실행은 같은 시각의 WEEK와 MONTH 행을 참조하지 않는다")
    void isolatesHourRowsFromRollups() throws Exception {
      LocalDateTime hourPrevious = PREV_BUCKET.minusHours(1);
      jdbc.update(
          """
          INSERT INTO alcohol_interest_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             view_count, cumulative_view_count)
          VALUES (1, 'HOUR', ?, ?, NULL, 3, 3),
                 (1, 'WEEK', ?, ?, NULL, 90, 90),
                 (1, 'MONTH', ?, ?, NULL, 99, 99),
                 (1, 'WEEK', ?, ?, NULL, 80, 170),
                 (1, 'MONTH', ?, ?, NULL, 88, 187)
          """,
          hourPrevious,
          hourPrevious,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);
      alcoholViewCounter.put(BUCKET, Map.of(1L, 2L));

      run(interestTasklet(), BUCKET);
      run(interestTasklet(), BUCKET);

      List<Map<String, Object>> hourRows = rowsOf("alcohol_interest_observations", "HOUR");
      assertThat(hourRows).hasSize(2);
      assertThat(hourRows.get(1).get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(hourPrevious));
      assertThat(hourRows.get(1).get("view_count")).isEqualTo(2L);
      assertThat(hourRows.get(1).get("cumulative_view_count")).isEqualTo(5L);
      assertThat(rowsOf("alcohol_interest_observations", "WEEK").get(1).get("view_count"))
          .isEqualTo(80L);
      assertThat(rowsOf("alcohol_interest_observations", "MONTH").get(1).get("view_count"))
          .isEqualTo(88L);
    }
  }

  @Nested
  @DisplayName("평가도")
  class Rating {

    @Test
    @DisplayName("0점은 미평가로 보고 집계에서 뺀다")
    void excludesZeroRatings() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.5)");
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,2,0)");

      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("rating_count")).isEqualTo(1L);
      assertThat(((Number) rows.get(0).get("rating_sum")).doubleValue()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("값이 그대로면 두 번째 관측에서 행을 남기지 않는다")
    void skipsWhenValueUnchanged() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");

      run(new RatingObservationTasklet(writer), PREV_BUCKET);
      run(new RatingObservationTasklet(writer), BUCKET);

      assertThat(rowsOf("alcohol_rating_observations")).hasSize(1);
    }

    @Test
    @DisplayName("값이 바뀌면 직전 대비 증감을 함께 기록한다")
    void recordsDeltaWhenValueChanges() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), PREV_BUCKET);

      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,2,5.0)");
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("delta_rating_count")).isEqualTo(1L);
      assertThat(((Number) rows.get(1).get("delta_rating_sum")).doubleValue()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("같은 버킷을 다시 관측해도 증감이 어긋나지 않는다")
    void isIdempotentWithinSameBucket() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), PREV_BUCKET);

      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,2,5.0)");
      run(new RatingObservationTasklet(writer), BUCKET);
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("rating_count")).isEqualTo(2L);
      assertThat(rows.get(1).get("delta_rating_count"))
          .as("재실행이 직전 버킷을 다시 보므로 증감은 그대로여야 한다")
          .isEqualTo(1L);
    }

    @Test
    @DisplayName("재실행하면 값이 줄어든 것도 정정한다")
    void reRunCorrectsDownward() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), PREV_BUCKET);

      // 1회차: 평점이 3개인 상태로 기록된다
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,2,4.0)");
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,3,4.0)");
      run(new RatingObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_rating_observations").get(1).get("rating_count")).isEqualTo(3L);

      // 두 건이 취소되어 직전 버킷과 같은 값으로 돌아간 상태에서 재실행한다
      jdbc.update("DELETE FROM ratings WHERE user_id IN (2,3)");
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("rating_count"))
          .as("직전과 값이 같다고 건너뛰면 1회차의 잘못된 3이 남는다")
          .isEqualTo(1L);
      assertThat(rows.get(1).get("delta_rating_count")).isEqualTo(0L);
    }

    @Test
    @DisplayName("직전 HOUR 없이 첫 관측한 값이 재실행 전에 사라지면 0으로 정정한다")
    void rerunCorrectsFirstObservationWithNoPreviousBucket() throws Exception {
      // 직전 HOUR 관측이 없는 상태에서 첫 실행이 nonzero로 적재된다
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_rating_observations").get(0).get("rating_count")).isEqualTo(1L);

      // 원본이 사라진 채 같은 버킷을 재실행한다 — previous는 여전히 비어 있다
      jdbc.update("DELETE FROM ratings WHERE alcohol_id = 1");
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows)
          .as("행 수가 늘지 않고 기존 행이 0으로 정정된다")
          .hasSize(1);
      assertThat(rows.get(0).get("rating_count")).isEqualTo(0L);
      assertThat(((Number) rows.get(0).get("rating_sum")).doubleValue()).isEqualTo(0.0);
      assertThat(rows.get(0).get("delta_rating_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("prev_bucket_at")).isNull();
    }

    @Test
    @DisplayName("직전 HOUR가 0이어도 이번 버킷에 잘못된 nonzero가 남아 있으면 정정한다")
    void rerunCorrectsEvenWhenPreviousBucketIsZero() throws Exception {
      // 직전 HOUR는 명시적으로 0으로 기록돼 있다
      jdbc.update(
          """
          INSERT INTO alcohol_rating_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             rating_count, rating_sum, delta_rating_count, delta_rating_sum)
          VALUES (1, 'HOUR', ?, ?, NULL, 0, 0.0, 0, 0.0)
          """,
          PREV_BUCKET,
          PREV_BUCKET);

      // 이번 버킷은 첫 실행에서 nonzero로 잘못 적재된다
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_rating_observations").get(1).get("rating_count")).isEqualTo(1L);

      // 원본이 사라진 채 같은 버킷을 재실행한다 — previous(직전 HOUR)는 여전히 0이다
      jdbc.update("DELETE FROM ratings WHERE alcohol_id = 1");
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("rating_count"))
          .as("직전이 0이라고 건너뛰면 1회차의 잘못된 1이 남는다")
          .isEqualTo(0L);
      assertThat(rows.get(1).get("delta_rating_count")).isEqualTo(0L);
    }

    @Test
    @DisplayName("평점이 모두 사라지면 0으로 떨어뜨려 기록한다")
    void writesZeroWhenAllRatingsDisappear() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), PREV_BUCKET);

      // 평점 삭제 — 집계 결과에서 아예 빠진다
      jdbc.update("DELETE FROM ratings WHERE alcohol_id = 1");
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_rating_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("rating_count")).isEqualTo(0L);
      assertThat(rows.get(1).get("delta_rating_count")).isEqualTo(-1L);
    }

    @Test
    @DisplayName("같은 시각의 주간·월간 행과 격리해 시간 관측을 재실행한다")
    void isolatesHourRowsFromRollups() throws Exception {
      LocalDateTime hourPrevious = PREV_BUCKET.minusHours(1);
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,2,5.0)");
      jdbc.update(
          """
          INSERT INTO alcohol_rating_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             rating_count, rating_sum, delta_rating_count, delta_rating_sum)
          VALUES (1, 'HOUR', ?, ?, NULL, 1, 4.0, 1, 4.0),
                 (1, 'WEEK', ?, ?, NULL, 90, 90.0, 90, 90.0),
                 (1, 'MONTH', ?, ?, NULL, 99, 99.0, 99, 99.0),
                 (1, 'WEEK', ?, ?, NULL, 80, 80.0, -10, -10.0),
                 (1, 'MONTH', ?, ?, NULL, 88, 88.0, -11, -11.0)
          """,
          hourPrevious,
          hourPrevious,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);

      run(new RatingObservationTasklet(writer), BUCKET);
      run(new RatingObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> hourRows = rowsOf("alcohol_rating_observations", "HOUR");
      assertThat(hourRows).hasSize(2);
      assertThat(hourRows.get(1).get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(hourPrevious));
      assertThat(hourRows.get(1).get("rating_count")).isEqualTo(2L);
      assertThat(hourRows.get(1).get("delta_rating_count")).isEqualTo(1L);
      assertThat(rowsOf("alcohol_rating_observations", "WEEK").get(1).get("rating_count"))
          .isEqualTo(80L);
      assertThat(rowsOf("alcohol_rating_observations", "MONTH").get(1).get("rating_count"))
          .isEqualTo(88L);
    }
  }

  @Nested
  @DisplayName("선호도")
  class Pick {

    @Test
    @DisplayName("PICK만 세고 UNPICK은 철회로 따로 센다")
    void countsPickAndUnpickSeparately() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,2,'PICK')");
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,3,'UNPICK')");

      run(new PickObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_pick_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("pick_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("unpick_count")).isEqualTo(1L);
    }

    @Test
    @DisplayName("픽이 취소되면 증감이 음수로 기록된다")
    void recordsNegativeDeltaWhenPickWithdrawn() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,2,'PICK')");
      run(new PickObservationTasklet(writer), PREV_BUCKET);

      jdbc.update("UPDATE picks SET status='UNPICK' WHERE user_id=2");
      run(new PickObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_pick_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("pick_count")).isEqualTo(1L);
      assertThat(rows.get(1).get("delta_pick_count")).isEqualTo(-1L);
    }

    @Test
    @DisplayName("직전 HOUR 없이 첫 관측한 값이 재실행 전에 사라지면 0으로 정정한다")
    void rerunCorrectsFirstObservationWithNoPreviousBucket() throws Exception {
      // 직전 HOUR 관측이 없는 상태에서 첫 실행이 nonzero로 적재된다
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_pick_observations").get(0).get("pick_count")).isEqualTo(1L);

      // 원본이 사라진 채 같은 버킷을 재실행한다 — previous는 여전히 비어 있다
      jdbc.update("DELETE FROM picks WHERE alcohol_id = 1");
      run(new PickObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_pick_observations");
      assertThat(rows)
          .as("행 수가 늘지 않고 기존 행이 0으로 정정된다")
          .hasSize(1);
      assertThat(rows.get(0).get("pick_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("unpick_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("delta_pick_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("prev_bucket_at")).isNull();
    }

    @Test
    @DisplayName("같은 시각의 주간·월간 행과 격리해 시간 관측을 재실행한다")
    void isolatesHourRowsFromRollups() throws Exception {
      LocalDateTime hourPrevious = PREV_BUCKET.minusHours(1);
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,2,'PICK')");
      jdbc.update(
          """
          INSERT INTO alcohol_pick_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             pick_count, unpick_count, delta_pick_count)
          VALUES (1, 'HOUR', ?, ?, NULL, 1, 0, 1),
                 (1, 'WEEK', ?, ?, NULL, 90, 9, 90),
                 (1, 'MONTH', ?, ?, NULL, 99, 9, 99),
                 (1, 'WEEK', ?, ?, NULL, 80, 8, -10),
                 (1, 'MONTH', ?, ?, NULL, 88, 8, -11)
          """,
          hourPrevious,
          hourPrevious,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);

      run(new PickObservationTasklet(writer), BUCKET);
      run(new PickObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> hourRows = rowsOf("alcohol_pick_observations", "HOUR");
      assertThat(hourRows).hasSize(2);
      assertThat(hourRows.get(1).get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(hourPrevious));
      assertThat(hourRows.get(1).get("pick_count")).isEqualTo(2L);
      assertThat(hourRows.get(1).get("delta_pick_count")).isEqualTo(1L);
      assertThat(rowsOf("alcohol_pick_observations", "WEEK").get(1).get("pick_count"))
          .isEqualTo(80L);
      assertThat(rowsOf("alcohol_pick_observations", "MONTH").get(1).get("pick_count"))
          .isEqualTo(88L);
    }
  }

  @Nested
  @DisplayName("참여도")
  class Engagement {

    @Test
    @DisplayName("좋아요와 댓글이 서로를 부풀리지 않는다")
    void likesAndRepliesDoNotMultiplyEachOther() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,2,'LIKE')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,1,'NORMAL')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,2,'NORMAL')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,3,'NORMAL')");

      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(1);
      // 조인을 한 번에 했다면 좋아요 6, 댓글 6이 됐을 것이다
      assertThat(rows.get(0).get("like_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("reply_count")).isEqualTo(3L);
      assertThat(rows.get(0).get("review_count")).isEqualTo(1L);
    }

    @Test
    @DisplayName("네 지표의 증감을 각각 따로 기록한다")
    void recordsEachDeltaSeparately() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      run(new EngagementObservationTasklet(writer), PREV_BUCKET);

      // 리뷰 +1, 좋아요 +2, 취소 +1, 댓글 +3
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (11,1,2,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,2,'LIKE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (11,3,'LIKE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (11,4,'DISLIKE')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,1,'NORMAL')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (11,2,'NORMAL')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (11,3,'NORMAL')");
      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(2);
      Map<String, Object> now = rows.get(1);
      assertThat(now.get("review_count")).isEqualTo(2L);
      assertThat(now.get("like_count")).isEqualTo(3L);
      assertThat(now.get("dislike_count")).isEqualTo(1L);
      assertThat(now.get("reply_count")).isEqualTo(3L);
      // 지표별 증감이 서로 섞이지 않아야 한다
      assertThat(now.get("delta_review_count")).isEqualTo(1L);
      assertThat(now.get("delta_like_count")).isEqualTo(2L);
      assertThat(now.get("delta_dislike_count")).isEqualTo(1L);
      assertThat(now.get("delta_reply_count")).isEqualTo(3L);
    }

    @Test
    @DisplayName("값이 그대로면 두 번째 관측에서 행을 남기지 않는다")
    void skipsWhenUnchanged() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      run(new EngagementObservationTasklet(writer), PREV_BUCKET);
      run(new EngagementObservationTasklet(writer), BUCKET);

      assertThat(rowsOf("alcohol_engagement_observations")).hasSize(1);
    }

    @Test
    @DisplayName("리뷰가 모두 비공개로 바뀌면 0으로 떨어뜨려 기록한다")
    void writesZeroWhenAllReviewsBecomePrivate() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      run(new EngagementObservationTasklet(writer), PREV_BUCKET);

      // 집계 결과에서 아예 빠진다 — 남기지 않으면 최종 적재가 과거 값을 계속 끌어온다
      jdbc.update("UPDATE reviews SET status='PRIVATE' WHERE id=10");
      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("review_count")).isEqualTo(0L);
      assertThat(rows.get(1).get("like_count")).isEqualTo(0L);
      assertThat(rows.get(1).get("delta_review_count")).isEqualTo(-1L);
      assertThat(rows.get(1).get("delta_like_count")).isEqualTo(-1L);
    }

    @Test
    @DisplayName("재실행하면 줄어든 값도 정정한다")
    void reRunCorrectsDownward() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      run(new EngagementObservationTasklet(writer), PREV_BUCKET);

      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (11,1,2,'PUBLIC','ACTIVE')");
      run(new EngagementObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_engagement_observations").get(1).get("review_count"))
          .isEqualTo(2L);

      // 직전 버킷과 같은 값으로 돌아간 상태에서 재실행한다
      jdbc.update("DELETE FROM reviews WHERE id=11");
      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("review_count"))
          .as("직전과 값이 같다고 건너뛰면 1회차의 잘못된 2가 남는다")
          .isEqualTo(1L);
      assertThat(rows.get(1).get("delta_review_count")).isEqualTo(0L);
    }

    @Test
    @DisplayName("직전 HOUR 없이 첫 관측한 값이 재실행 전에 사라지면 0으로 정정한다")
    void rerunCorrectsFirstObservationWithNoPreviousBucket() throws Exception {
      // 직전 HOUR 관측이 없는 상태에서 첫 실행이 nonzero로 적재된다
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      run(new EngagementObservationTasklet(writer), BUCKET);
      assertThat(rowsOf("alcohol_engagement_observations").get(0).get("review_count"))
          .isEqualTo(1L);

      // 원본이 사라진 채 같은 버킷을 재실행한다 — previous는 여전히 비어 있다
      jdbc.update("DELETE FROM likes WHERE review_id = 10");
      jdbc.update("UPDATE reviews SET status='PRIVATE' WHERE id=10");
      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows)
          .as("행 수가 늘지 않고 기존 행이 0으로 정정된다")
          .hasSize(1);
      assertThat(rows.get(0).get("review_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("like_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("delta_review_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("delta_like_count")).isEqualTo(0L);
      assertThat(rows.get(0).get("prev_bucket_at")).isNull();
    }

    @Test
    @DisplayName("삭제·차단·숨김 댓글은 참여로 세지 않는다")
    void excludesNonNormalReplies() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,1,'NORMAL')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,2,'DELETED')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,3,'BLOCKED')");
      jdbc.update("INSERT INTO review_replies (review_id, user_id, status) VALUES (10,4,'HIDDEN')");

      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("reply_count"))
          .as("NORMAL 하나만 남아야 한다")
          .isEqualTo(1L);
    }

    @Test
    @DisplayName("비공개·삭제 리뷰와 취소된 좋아요는 참여로 세지 않는다")
    void excludesPrivateDeletedAndDislike() throws Exception {
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE')");
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (11,1,2,'PRIVATE','ACTIVE')");
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (12,1,3,'PUBLIC','DELETED')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,2,'DISLIKE')");
      // 비공개 리뷰에 달린 좋아요는 집계 대상이 아니다
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (11,3,'LIKE')");

      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_engagement_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("review_count")).isEqualTo(1L);
      assertThat(rows.get(0).get("like_count")).isEqualTo(1L);
      assertThat(rows.get(0).get("dislike_count")).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 시각의 주간·월간 행과 격리해 시간 관측을 재실행한다")
    void isolatesHourRowsFromRollups() throws Exception {
      LocalDateTime hourPrevious = PREV_BUCKET.minusHours(1);
      jdbc.update(
          "INSERT INTO reviews (id, alcohol_id, user_id, status, active_status)"
              + " VALUES (10,1,1,'PUBLIC','ACTIVE'), (11,1,2,'PUBLIC','ACTIVE')");
      jdbc.update("INSERT INTO likes (review_id, user_id, status) VALUES (10,1,'LIKE')");
      jdbc.update(
          """
          INSERT INTO alcohol_engagement_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             review_count, like_count, dislike_count, reply_count,
             delta_review_count, delta_like_count, delta_dislike_count, delta_reply_count)
          VALUES (1, 'HOUR', ?, ?, NULL, 1, 0, 0, 0, 1, 0, 0, 0),
                 (1, 'WEEK', ?, ?, NULL, 90, 90, 9, 90, 90, 90, 9, 90),
                 (1, 'MONTH', ?, ?, NULL, 99, 99, 9, 99, 99, 99, 9, 99),
                 (1, 'WEEK', ?, ?, NULL, 80, 80, 8, 80, -10, -10, -1, -10),
                 (1, 'MONTH', ?, ?, NULL, 88, 88, 8, 88, -11, -11, -1, -11)
          """,
          hourPrevious,
          hourPrevious,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          PREV_BUCKET,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);

      run(new EngagementObservationTasklet(writer), BUCKET);
      run(new EngagementObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> hourRows = rowsOf("alcohol_engagement_observations", "HOUR");
      assertThat(hourRows).hasSize(2);
      assertThat(hourRows.get(1).get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(hourPrevious));
      assertThat(hourRows.get(1).get("review_count")).isEqualTo(2L);
      assertThat(hourRows.get(1).get("like_count")).isEqualTo(1L);
      assertThat(hourRows.get(1).get("delta_review_count")).isEqualTo(1L);
      assertThat(hourRows.get(1).get("delta_like_count")).isEqualTo(1L);
      assertThat(
              rowsOf("alcohol_engagement_observations", "WEEK").get(1).get("review_count"))
          .isEqualTo(80L);
      assertThat(
              rowsOf("alcohol_engagement_observations", "MONTH").get(1).get("review_count"))
          .isEqualTo(88L);
    }
  }

  @Nested
  @DisplayName("최종 적재")
  class Snapshot {

    private PopularitySnapshotTasklet snapshotTasklet() {
      return new PopularitySnapshotTasklet(writer, new PopularityObservationProperties());
    }

    @Test
    @DisplayName("한 축만 관측된 주류도 최종 행을 얻는다")
    void unionOfAllAxes() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (2,1,'PICK')");
      run(new RatingObservationTasklet(writer), BUCKET);
      run(new PickObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(0).get("rating_value")).isEqualTo(1L);
      assertThat(rows.get(0).get("pick_value")).isEqualTo(0L);
      assertThat(rows.get(1).get("pick_value")).isEqualTo(1L);
    }

    @Test
    @DisplayName("상태 축은 이번 버킷에 관측이 없으면 직전 값과 출처 버킷을 끌어온다")
    void stateAxisCarriesForwardWithSource() throws Exception {
      jdbc.update("INSERT INTO ratings (alcohol_id, user_id, rating) VALUES (1,1,4.0)");
      run(new RatingObservationTasklet(writer), PREV_BUCKET);
      // 이번 버킷에는 평가도 관측이 없다 (값 불변이라 희소 저장으로 생략됨)
      run(new RatingObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("rating_value")).isEqualTo(1L);
      assertThat(rows.get(0).get("rating_source_bucket_at"))
          .as("끌어온 값이 언제 관측된 것인지 남아야 갭을 알 수 있다")
          .isNotNull();
    }

    @Test
    @DisplayName("흐름 축은 이번 버킷에 관측이 없으면 직전 값을 끌어오지 않고 0이다")
    void flowAxisDoesNotCarryForward() throws Exception {
      alcoholViewCounter.put(PREV_BUCKET, Map.of(1L, 1L));
      run(interestTasklet(), PREV_BUCKET);
      // 이번 구간에는 조회가 없다
      run(interestTasklet(), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("interest_value"))
          .as("조회가 없던 시간에 과거 조회수가 찍히면 관심이 식지 않는 것처럼 보인다")
          .isEqualTo(0L);
      assertThat(rows.get(0).get("interest_source_bucket_at")).isNull();
    }

    @Test
    @DisplayName("축 하나가 기준값의 절반이면 그 축 가중치의 절반만 점수에 실린다")
    void scoreReflectsNormalizationAndWeight() throws Exception {
      // 기본 정규화 기준: pick=200, 가중치 0.25 → 100건이면 0.5 * 0.25 = 0.125
      for (int i = 1; i <= 100; i++) {
        jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,?,'PICK')", i);
      }
      run(new PickObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(rows).hasSize(1);
      assertThat(((Number) rows.get(0).get("pick_value")).longValue()).isEqualTo(100L);
      assertThat(new java.math.BigDecimal(rows.get(0).get("pick_score").toString()))
          .isEqualByComparingTo("0.5");
      assertThat(new java.math.BigDecimal(rows.get(0).get("popularity_score").toString()))
          .as("나머지 세 축은 0이므로 0.5 * 0.25만 남는다")
          .isEqualByComparingTo("0.125");
    }

    @Test
    @DisplayName("기준값을 넘겨도 축 점수는 1을 넘지 않는다")
    void axisScoreIsCappedAtOne() throws Exception {
      for (int i = 1; i <= 300; i++) {
        jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,?,'PICK')", i);
      }
      run(new PickObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(new java.math.BigDecimal(rows.get(0).get("pick_score").toString()))
          .isEqualByComparingTo("1.0");
      assertThat(new java.math.BigDecimal(rows.get(0).get("popularity_score").toString()))
          .isEqualByComparingTo("0.25");
    }

    @Test
    @DisplayName("삭제된 주류는 최종 적재 대상에서 빠진다")
    void excludesDeletedAlcohol() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (3,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);
      jdbc.update("UPDATE alcohols SET deleted_at = ? WHERE id = 3", BUCKET);

      run(snapshotTasklet(), BUCKET);

      assertThat(rowsOf("alcohol_popularity_snapshots")).isEmpty();
    }

    @Test
    @DisplayName("같은 버킷을 다시 적재해도 행이 늘지 않는다")
    void isIdempotentWithinSameBucket() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);
      run(snapshotTasklet(), BUCKET);

      assertThat(rowsOf("alcohol_popularity_snapshots")).hasSize(1);
    }

    @Test
    @DisplayName("시간 Snapshot은 HOUR 기간 메타데이터와 직전 시간 버킷을 기록한다")
    void writesHourMetadataAndPreviousBucket() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), PREV_BUCKET);
      run(snapshotTasklet(), PREV_BUCKET);

      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,2,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);
      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots", "HOUR");
      assertThat(rows).hasSize(2);
      assertThat(rows).allSatisfy(row -> assertThat(row.get("observed_at")).isNotNull());
      assertThat(rows.get(0).get("prev_bucket_at")).isNull();
      assertThat(rows.get(1).get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(PREV_BUCKET));
    }

    @Test
    @DisplayName("시간 Snapshot은 같은 시각의 주간·월간 축 행을 읽지 않는다")
    void readsOnlyHourAxes() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);
      jdbc.update(
          """
          INSERT INTO alcohol_pick_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, prev_bucket_at,
             pick_count, unpick_count, delta_pick_count)
          VALUES (1, 'WEEK', ?, ?, NULL, 90, 0, 90),
                 (1, 'MONTH', ?, ?, NULL, 99, 0, 99)
          """,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots", "HOUR");
      assertThat(rows)
          .singleElement()
          .satisfies(row -> assertThat(row.get("pick_value")).isEqualTo(1L));
    }

    @Test
    @DisplayName("같은 시간 재실행에서 원천 행이 사라지면 기존 Snapshot 값을 0으로 정정한다")
    void rerunCorrectsMissingSourceToZero() throws Exception {
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);
      run(snapshotTasklet(), BUCKET);
      jdbc.update("DELETE FROM alcohol_pick_observations WHERE bucket_granularity = 'HOUR'");

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots", "HOUR");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("pick_value")).isEqualTo(0L);
    }

    @Test
    @DisplayName("시간 Snapshot 재실행은 같은 시각의 주간·월간 Snapshot을 변경하지 않는다")
    void preservesRollupSnapshotsAtSameTime() throws Exception {
      jdbc.update(
          """
          INSERT INTO alcohol_popularity_snapshots
            (alcohol_id, bucket_granularity, bucket_at, observed_at)
          VALUES (1, 'WEEK', ?, ?), (1, 'MONTH', ?, ?)
          """,
          BUCKET,
          BUCKET,
          BUCKET,
          BUCKET);
      jdbc.update("INSERT INTO picks (alcohol_id, user_id, status) VALUES (1,1,'PICK')");
      run(new PickObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);
      run(snapshotTasklet(), BUCKET);

      assertThat(rowsOf("alcohol_popularity_snapshots", "HOUR")).hasSize(1);
      assertThat(rowsOf("alcohol_popularity_snapshots", "WEEK")).hasSize(1);
      assertThat(rowsOf("alcohol_popularity_snapshots", "MONTH")).hasSize(1);
    }

    @Test
    @DisplayName("주간 Snapshot은 WEEK 축과 정규화 기준 및 같은 단위 직전 버킷을 사용한다")
    void writesWeekSnapshotFromWeekAxes() throws Exception {
      LocalDateTime week = LocalDateTime.of(2026, 8, 17, 0, 0);
      LocalDateTime previousWeek = week.minusWeeks(1);
      jdbc.update(
          """
          INSERT INTO alcohol_interest_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, view_count)
          VALUES (1, 'HOUR', ?, ?, 999), (1, 'WEEK', ?, ?, 20)
          """,
          week,
          week,
          week,
          week);
      jdbc.update(
          """
          INSERT INTO alcohol_rating_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, rating_count)
          VALUES (1, 'WEEK', ?, ?, 4)
          """,
          week,
          week);
      jdbc.update(
          """
          INSERT INTO alcohol_pick_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at, pick_count)
          VALUES (1, 'WEEK', ?, ?, 6)
          """,
          week,
          week);
      jdbc.update(
          """
          INSERT INTO alcohol_engagement_observations
            (alcohol_id, bucket_granularity, bucket_at, observed_at,
             review_count, like_count, reply_count)
          VALUES (1, 'WEEK', ?, ?, 2, 3, 4)
          """,
          week,
          week);
      jdbc.update(
          """
          INSERT INTO alcohol_popularity_snapshots
            (alcohol_id, bucket_granularity, bucket_at, observed_at)
          VALUES (1, 'WEEK', ?, ?), (1, 'HOUR', ?, ?)
          """,
          previousWeek,
          previousWeek,
          week.minusHours(1),
          week.minusHours(1));

      PopularityObservationProperties properties = new PopularityObservationProperties();
      properties.getReference().getWeek().setInterest(40L);
      properties.getReference().getWeek().setRating(8L);
      properties.getReference().getWeek().setPick(12L);
      properties.getReference().getWeek().setEngagement(18L);

      run(
          new PopularitySnapshotTasklet(writer, properties),
          BucketGranularity.WEEK,
          week);

      assertThat(rowsOf("alcohol_popularity_snapshots", "WEEK"))
          .hasSize(2)
          .last()
          .satisfies(
              row -> {
                assertThat(row.get("prev_bucket_at")).isEqualTo(Timestamp.valueOf(previousWeek));
                assertThat(row.get("interest_value")).isEqualTo(20L);
                assertThat(row.get("rating_value")).isEqualTo(4L);
                assertThat(row.get("pick_value")).isEqualTo(6L);
                assertThat(row.get("engagement_value")).isEqualTo(9L);
                assertThat(new java.math.BigDecimal(row.get("popularity_score").toString()))
                    .isEqualByComparingTo("0.5");
              });
    }
  }

  private static final class InMemoryAlcoholViewCounter implements AlcoholViewCounter {

    private final Map<LocalDateTime, Map<Long, Long>> countsByBucket = new HashMap<>();
    private final List<LocalDateTime> requestedBuckets = new ArrayList<>();

    @Override
    public void increment(Long alcoholId) {
      throw new UnsupportedOperationException("배치 테스트는 조회만 지원합니다.");
    }

    @Override
    public Map<Long, Long> findCounts(LocalDateTime bucketAt) {
      requestedBuckets.add(bucketAt);
      return countsByBucket.getOrDefault(bucketAt, Map.of());
    }

    private void put(LocalDateTime bucketAt, Map<Long, Long> counts) {
      countsByBucket.put(bucketAt, Map.copyOf(counts));
    }
  }
}
