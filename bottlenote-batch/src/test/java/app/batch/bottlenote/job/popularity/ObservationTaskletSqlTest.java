package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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

  @BeforeEach
  void setUp() {
    jdbc = ObservationSqlSupport.freshDatabase();
    writer = new ObservationWriter(jdbc);
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

  private List<Map<String, Object>> rowsOf(String table) {
    return jdbc.queryForList("SELECT * FROM " + table + " ORDER BY alcohol_id, bucket_at");
  }

  @Nested
  @DisplayName("관심도")
  class Interest {

    @Test
    @DisplayName("구간 안에 갱신된 조회만 세고 구간 밖은 세지 않는다")
    void countsOnlyViewsInsideWindow() throws Exception {
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          1L, 1L, BUCKET.minusMinutes(30));
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          2L, 1L, BUCKET.minusMinutes(10));
      // 구간 밖 — 2시간 전
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          3L, 1L, BUCKET.minusHours(2));

      run(new InterestObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("viewer_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("cumulative_viewer_count")).isEqualTo(2L);
    }

    @Test
    @DisplayName("조회가 없는 주류는 행을 남기지 않는다")
    void writesNothingForUnviewedAlcohol() throws Exception {
      run(new InterestObservationTasklet(writer), BUCKET);

      assertThat(rowsOf("alcohol_interest_observations")).isEmpty();
    }

    @Test
    @DisplayName("같은 버킷을 다시 관측해도 누적이 이중 계산되지 않는다")
    void isIdempotentWithinSameBucket() throws Exception {
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          1L, 1L, BUCKET.minusMinutes(10));
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          2L, 1L, BUCKET.minusMinutes(5));

      // misfire 복구나 수동 재실행으로 같은 버킷이 두 번 처리될 수 있다
      run(new InterestObservationTasklet(writer), BUCKET);
      run(new InterestObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("viewer_count")).isEqualTo(2L);
      assertThat(rows.get(0).get("cumulative_viewer_count"))
          .as("직전 누적을 다시 더하면 4가 된다")
          .isEqualTo(2L);
    }

    @Test
    @DisplayName("누적은 직전 관측 누적에 이번 구간을 더한 값이다")
    void cumulativeAddsOnTopOfPrevious() throws Exception {
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          1L, 1L, PREV_BUCKET.minusMinutes(10));
      run(new InterestObservationTasklet(writer), PREV_BUCKET);

      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          2L, 1L, BUCKET.minusMinutes(10));
      run(new InterestObservationTasklet(writer), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_interest_observations");
      assertThat(rows).hasSize(2);
      assertThat(rows.get(1).get("viewer_count")).isEqualTo(1L);
      assertThat(rows.get(1).get("cumulative_viewer_count")).isEqualTo(2L);
      assertThat(rows.get(1).get("prev_bucket_at")).isNotNull();
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
      jdbc.update(
          "INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at) VALUES (?,?,?)",
          1L, 1L, PREV_BUCKET.minusMinutes(10));
      run(new InterestObservationTasklet(writer), PREV_BUCKET);
      // 이번 구간에는 조회가 없다
      run(new InterestObservationTasklet(writer), BUCKET);

      run(snapshotTasklet(), BUCKET);

      List<Map<String, Object>> rows = rowsOf("alcohol_popularity_snapshots");
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("interest_value"))
          .as("조회가 없던 시간에 과거 조회수가 찍히면 관심이 식지 않는 것처럼 보인다")
          .isEqualTo(0L);
      assertThat(rows.get(0).get("interest_source_bucket_at")).isNull();
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
  }
}
