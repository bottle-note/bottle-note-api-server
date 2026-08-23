package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("batch")
@Testcontainers
@DisplayName("[batch] 인기도 WEEK/MONTH MySQL 롤업")
class PopularityRollupTaskletMySqlIntegrationTest {

  private static final LocalDateTime WEEK = LocalDateTime.of(2026, 8, 3, 0, 0);

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
          .withDatabaseName("popularity_rollup")
          .withUsername("root")
          .withPassword("root");

  private static JdbcTemplate jdbc;
  private PopularityRollupTasklet tasklet;

  @BeforeAll
  static void createSchema() throws IOException {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    jdbc.execute(
        "CREATE TABLE alcohols (id BIGINT NOT NULL PRIMARY KEY, deleted_at DATETIME(6) NULL)");

    Path migration =
        Path.of("..", "git.environment-variables", "storage", "db", "migration")
            .resolve("V14__add_popularity_observation_tables.sql")
            .toAbsolutePath()
            .normalize();
    String ddl = Files.readString(migration, StandardCharsets.UTF_8);
    for (String statement : ddl.split(";")) {
      String sql = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
      if (!sql.isEmpty()) {
        jdbc.execute(sql);
      }
    }
  }

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM alcohol_popularity_snapshots");
    jdbc.update("DELETE FROM alcohol_engagement_observations");
    jdbc.update("DELETE FROM alcohol_pick_observations");
    jdbc.update("DELETE FROM alcohol_rating_observations");
    jdbc.update("DELETE FROM alcohol_interest_observations");
    jdbc.update("DELETE FROM alcohols");
    tasklet = new PopularityRollupTasklet(new ObservationWriter(jdbc));
  }

  @Test
  @DisplayName("주 경계 안의 HOUR만 집계하고 활성 주류를 0까지 조밀하게 적재한다")
  void rollsUpWeekFromHourRowsWithDenseZeros() throws Exception {
    seedAlcohols();
    seedWeekHourRows();

    run(BucketGranularity.WEEK, WEEK);

    assertThat(count("alcohol_interest_observations", "WEEK")).isEqualTo(2);
    assertThat(count("alcohol_rating_observations", "WEEK")).isEqualTo(2);
    assertThat(count("alcohol_pick_observations", "WEEK")).isEqualTo(2);
    assertThat(count("alcohol_engagement_observations", "WEEK")).isEqualTo(2);

    Map<String, Object> interest = row("alcohol_interest_observations", "WEEK", 1L);
    assertThat(number(interest, "view_count")).isEqualTo(5L);
    assertThat(number(interest, "cumulative_view_count")).isEqualTo(15L);

    Map<String, Object> rating = row("alcohol_rating_observations", "WEEK", 1L);
    assertThat(number(rating, "rating_count")).isEqualTo(4L);
    assertThat(number(rating, "rating_sum")).isEqualTo(15L);
    assertThat(number(rating, "delta_rating_count")).isEqualTo(2L);
    assertThat(number(rating, "delta_rating_sum")).isEqualTo(8L);

    Map<String, Object> pick = row("alcohol_pick_observations", "WEEK", 1L);
    assertThat(number(pick, "pick_count")).isEqualTo(6L);
    assertThat(number(pick, "unpick_count")).isEqualTo(2L);
    assertThat(number(pick, "delta_pick_count")).isEqualTo(3L);

    Map<String, Object> engagement = row("alcohol_engagement_observations", "WEEK", 1L);
    assertThat(number(engagement, "review_count")).isEqualTo(7L);
    assertThat(number(engagement, "like_count")).isEqualTo(8L);
    assertThat(number(engagement, "dislike_count")).isEqualTo(1L);
    assertThat(number(engagement, "reply_count")).isEqualTo(9L);
    assertThat(number(engagement, "delta_review_count")).isEqualTo(2L);
    assertThat(number(engagement, "delta_like_count")).isEqualTo(3L);
    assertThat(number(engagement, "delta_dislike_count")).isEqualTo(1L);
    assertThat(number(engagement, "delta_reply_count")).isEqualTo(4L);

    assertDenseZero("alcohol_interest_observations", "view_count", "cumulative_view_count");
    assertDenseZero(
        "alcohol_rating_observations",
        "rating_count",
        "rating_sum",
        "delta_rating_count",
        "delta_rating_sum");
    assertDenseZero(
        "alcohol_pick_observations", "pick_count", "unpick_count", "delta_pick_count");
    assertDenseZero(
        "alcohol_engagement_observations",
        "review_count",
        "like_count",
        "dislike_count",
        "reply_count",
        "delta_review_count",
        "delta_like_count",
        "delta_dislike_count",
        "delta_reply_count");
  }

  @Test
  @DisplayName("같은 주 버킷 재실행은 기존 값을 절대값으로 복구하고 행을 늘리지 않는다")
  void rerunUpsertsAbsoluteValues() throws Exception {
    seedAlcohols();
    seedWeekHourRows();
    run(BucketGranularity.WEEK, WEEK);
    jdbc.update(
        "UPDATE alcohol_interest_observations SET view_count = 999, cumulative_view_count = 999 "
            + "WHERE bucket_granularity = 'WEEK'");
    jdbc.update(
        "UPDATE alcohol_rating_observations SET rating_count = 999, delta_rating_count = 999 "
            + "WHERE bucket_granularity = 'WEEK'");
    jdbc.update(
        "UPDATE alcohol_pick_observations SET pick_count = 999, delta_pick_count = 999 "
            + "WHERE bucket_granularity = 'WEEK'");
    jdbc.update(
        "UPDATE alcohol_engagement_observations SET review_count = 999, delta_review_count = 999 "
            + "WHERE bucket_granularity = 'WEEK'");

    run(BucketGranularity.WEEK, WEEK);

    assertThat(count("alcohol_interest_observations", "WEEK")).isEqualTo(2);
    assertThat(number(row("alcohol_interest_observations", "WEEK", 1L), "view_count"))
        .isEqualTo(5L);
    assertThat(number(row("alcohol_rating_observations", "WEEK", 1L), "rating_count"))
        .isEqualTo(4L);
    assertThat(number(row("alcohol_rating_observations", "WEEK", 1L), "delta_rating_count"))
        .isEqualTo(2L);
    assertThat(number(row("alcohol_pick_observations", "WEEK", 1L), "pick_count"))
        .isEqualTo(6L);
    assertThat(number(row("alcohol_pick_observations", "WEEK", 1L), "delta_pick_count"))
        .isEqualTo(3L);
    assertThat(number(row("alcohol_engagement_observations", "WEEK", 1L), "review_count"))
        .isEqualTo(7L);
    assertThat(
            number(
                row("alcohol_engagement_observations", "WEEK", 1L), "delta_review_count"))
        .isEqualTo(2L);
  }

  @Test
  @DisplayName("월 롤업은 달력 월의 시작을 포함하고 다음 달 시작을 제외한다")
  void rollsUpCalendarMonthBoundary() throws Exception {
    LocalDateTime month = LocalDateTime.of(2026, 2, 1, 0, 0);
    jdbc.update("INSERT INTO alcohols (id, deleted_at) VALUES (1, NULL)");
    insertInterest(LocalDateTime.of(2026, 1, 31, 23, 0), 1, 1);
    insertInterest(month, 2, 3);
    insertInterest(LocalDateTime.of(2026, 2, 28, 23, 0), 4, 7);
    insertInterest(LocalDateTime.of(2026, 3, 1, 0, 0), 8, 15);

    run(BucketGranularity.MONTH, month);

    Map<String, Object> result = row("alcohol_interest_observations", "MONTH", 1L);
    assertThat(number(result, "view_count")).isEqualTo(6L);
    assertThat(number(result, "cumulative_view_count")).isEqualTo(7L);
  }

  @Test
  @DisplayName("HOUR 단위는 롤업 입력으로 거부한다")
  void rejectsHourGranularity() {
    assertThatThrownBy(() -> run(BucketGranularity.HOUR, WEEK))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("HOUR는 롤업할 수 없습니다.");
  }

  private void run(BucketGranularity granularity, LocalDateTime bucketAt) throws Exception {
    JobParameters parameters =
        new JobParametersBuilder()
            .addString(PopularityRollupTasklet.GRANULARITY_PARAM, granularity.name())
            .addLocalDateTime(ObservationBucket.BUCKET_AT_PARAM, bucketAt)
            .toJobParameters();
    JobExecution jobExecution =
        MetaDataInstanceFactory.createJobExecution("popularityRollupJob", 1L, 1L, parameters);
    StepExecution stepExecution = jobExecution.createStepExecution("rollup");
    ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
    tasklet.execute(stepExecution.createStepContribution(), chunkContext);
  }

  private void seedAlcohols() {
    jdbc.update(
        "INSERT INTO alcohols (id, deleted_at) VALUES (1, NULL), (2, NULL), (3, ?)",
        Timestamp.valueOf(WEEK));
  }

  private void seedWeekHourRows() {
    insertInterest(WEEK.minusHours(1), 10, 10);
    insertInterest(WEEK, 2, 12);
    insertInterest(WEEK.plusDays(3), 3, 15);
    insertInterest(WEEK.plusWeeks(1), 99, 114);
    jdbc.update(
        "INSERT INTO alcohol_interest_observations "
            + "(alcohol_id, bucket_granularity, bucket_at, observed_at, view_count, cumulative_view_count) "
            + "VALUES (3, 'HOUR', ?, ?, 50, 50)",
        WEEK,
        WEEK);

    insertRating(WEEK.minusHours(1), 2, 7, 2, 7);
    insertRating(WEEK, 3, 11, 1, 4);
    insertRating(WEEK.plusDays(3), 4, 15, 1, 4);
    insertRating(WEEK.plusWeeks(1), 9, 40, 5, 25);

    insertPick(WEEK.minusHours(1), 3, 1, 3);
    insertPick(WEEK, 5, 1, 2);
    insertPick(WEEK.plusDays(3), 6, 2, 1);
    insertPick(WEEK.plusWeeks(1), 20, 3, 14);

    insertEngagement(WEEK.minusHours(1), 5, 5, 0, 5, 5, 5, 0, 5);
    insertEngagement(WEEK, 6, 6, 0, 7, 1, 1, 0, 2);
    insertEngagement(WEEK.plusDays(3), 7, 8, 1, 9, 1, 2, 1, 2);
    insertEngagement(WEEK.plusWeeks(1), 20, 20, 2, 20, 13, 12, 1, 11);
  }

  private void insertInterest(LocalDateTime bucketAt, long viewCount, long cumulative) {
    jdbc.update(
        "INSERT INTO alcohol_interest_observations "
            + "(alcohol_id, bucket_granularity, bucket_at, observed_at, view_count, cumulative_view_count) "
            + "VALUES (1, 'HOUR', ?, ?, ?, ?)",
        bucketAt,
        bucketAt,
        viewCount,
        cumulative);
  }

  private void insertRating(
      LocalDateTime bucketAt, long count, long sum, long deltaCount, long deltaSum) {
    jdbc.update(
        "INSERT INTO alcohol_rating_observations "
            + "(alcohol_id, bucket_granularity, bucket_at, observed_at, rating_count, rating_sum, "
            + "delta_rating_count, delta_rating_sum) VALUES (1, 'HOUR', ?, ?, ?, ?, ?, ?)",
        bucketAt,
        bucketAt,
        count,
        sum,
        deltaCount,
        deltaSum);
  }

  private void insertPick(
      LocalDateTime bucketAt, long pickCount, long unpickCount, long deltaPickCount) {
    jdbc.update(
        "INSERT INTO alcohol_pick_observations "
            + "(alcohol_id, bucket_granularity, bucket_at, observed_at, pick_count, unpick_count, "
            + "delta_pick_count) VALUES (1, 'HOUR', ?, ?, ?, ?, ?)",
        bucketAt,
        bucketAt,
        pickCount,
        unpickCount,
        deltaPickCount);
  }

  private void insertEngagement(
      LocalDateTime bucketAt,
      long review,
      long like,
      long dislike,
      long reply,
      long deltaReview,
      long deltaLike,
      long deltaDislike,
      long deltaReply) {
    jdbc.update(
        "INSERT INTO alcohol_engagement_observations "
            + "(alcohol_id, bucket_granularity, bucket_at, observed_at, review_count, like_count, "
            + "dislike_count, reply_count, delta_review_count, delta_like_count, "
            + "delta_dislike_count, delta_reply_count) "
            + "VALUES (1, 'HOUR', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        bucketAt,
        bucketAt,
        review,
        like,
        dislike,
        reply,
        deltaReview,
        deltaLike,
        deltaDislike,
        deltaReply);
  }

  private int count(String table, String granularity) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE bucket_granularity = ?",
        Integer.class,
        granularity);
  }

  private Map<String, Object> row(String table, String granularity, long alcoholId) {
    return jdbc.queryForMap(
        "SELECT * FROM "
            + table
            + " WHERE bucket_granularity = ? AND alcohol_id = ?",
        granularity,
        alcoholId);
  }

  private void assertDenseZero(String table, String... columns) {
    Map<String, Object> zero = row(table, "WEEK", 2L);
    for (String column : columns) {
      assertThat(number(zero, column)).isZero();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM "
                    + table
                    + " WHERE bucket_granularity = 'WEEK' AND alcohol_id = 3",
                Integer.class))
        .isZero();
  }

  private long number(Map<String, Object> row, String column) {
    return ((Number) row.get(column)).longValue();
  }
}
