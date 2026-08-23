package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * 보관 기간이 지난 HOUR 원본 정리.
 *
 * <p>나이 조건만 보고 지우면 롤업이 밀린 구간의 원본이 사라져 어떤 단위로도 복원할 수 없다. 삭제 자격을 주류마다 따로 판정하는지, 닫히지 않은 기간을 건너뛰는지가
 * 이 테스트의 핵심이다.
 */
@Tag("batch")
@DisplayName("[batch] HOUR 관측 보관 정리")
class HourObservationCleanupTaskletTest {

  /** 대상 월은 2026년 8월이다. 정리 기준은 9월 1일에서 45일을 뺀 7월 18일 00시가 된다. */
  private static final LocalDateTime TARGET_MONTH = LocalDateTime.of(2026, 8, 1, 0, 0);

  /** 2026-06-01은 월요일이라 주 경계와 달 경계가 같은 날 시작한다. */
  private static final LocalDateTime JUNE = LocalDateTime.of(2026, 6, 1, 0, 0);

  private JdbcTemplate jdbc;
  private PopularityObservationProperties properties;
  private HourObservationCleanupTasklet tasklet;

  @BeforeEach
  void setUp() {
    jdbc = ObservationSqlSupport.freshDatabase();
    jdbc.update("INSERT INTO alcohols (id, deleted_at) VALUES (1, NULL), (2, NULL)");
    properties = new PopularityObservationProperties();
    tasklet =
        new HourObservationCleanupTasklet(
            new ObservationWriter(jdbc),
            properties,
            new DataSourceTransactionManager(jdbc.getDataSource()));
  }

  @Test
  @DisplayName("주간과 월간 롤업이 모두 있는 주류의 HOUR만 다섯 영역에서 함께 지운다")
  void deletesFiveAreasForCoveredAlcoholOnly() {
    insertHour(1L, JUNE);
    insertHour(1L, JUNE.plusDays(9).plusHours(5));
    insertHour(2L, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE.plusWeeks(1));
    insertRollup(1L, BucketGranularity.MONTH, JUNE);
    insertRollup(2L, BucketGranularity.WEEK, JUNE);

    execute();

    // 2번 주류는 월간 롤업이 없어 같은 시간대라도 원본을 남긴다
    assertThat(hourKeys()).containsExactly(key(2L, JUNE));
    assertThat(rollupCount(BucketGranularity.WEEK)).isEqualTo(3);
    assertThat(rollupCount(BucketGranularity.MONTH)).isEqualTo(1);
  }

  @Test
  @DisplayName("다섯 영역 중 한 곳에 상위 롤업이 없으면 그 주류의 HOUR를 남긴다")
  void keepsHoursWhenAnyAreaMissesRollup() {
    insertHour(1L, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE);
    insertRollup(1L, BucketGranularity.MONTH, JUNE);
    jdbc.update(
        "DELETE FROM alcohol_pick_observations WHERE bucket_granularity = 'MONTH' AND alcohol_id = 1");

    execute();

    assertThat(hourKeys()).containsExactly(key(1L, JUNE));
  }

  @Test
  @DisplayName("기준 시각 안에서 닫히지 않은 주와 월의 HOUR는 지우지 않는다")
  void keepsHoursOfPeriodsNotClosedBeforeCutoff() {
    // 7월 13일 주는 7월 20일에 닫혀 기준 시각 7월 18일을 넘고, 7월 자체도 아직 닫히지 않았다
    LocalDateTime partialWeek = LocalDateTime.of(2026, 7, 13, 0, 0);
    insertHour(1L, partialWeek);
    insertRollup(1L, BucketGranularity.WEEK, partialWeek);
    insertRollup(1L, BucketGranularity.MONTH, LocalDateTime.of(2026, 7, 1, 0, 0));

    execute();

    assertThat(hourKeys()).containsExactly(key(1L, partialWeek));
  }

  @Test
  @DisplayName("기준 시각 이후의 최근 HOUR는 대상에서 제외한다")
  void keepsRecentHours() {
    LocalDateTime recent = LocalDateTime.of(2026, 8, 10, 0, 0);
    insertHour(1L, recent);
    insertRollup(1L, BucketGranularity.WEEK, recent);
    insertRollup(1L, BucketGranularity.MONTH, TARGET_MONTH);

    execute();

    assertThat(hourKeys()).containsExactly(key(1L, recent));
  }

  @Test
  @DisplayName("보관 일수를 늘리면 같은 원본이 정리 대상에서 빠진다")
  void respectsConfiguredRetentionDays() {
    properties.getRetention().setHourDays(120);
    insertHour(1L, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE);
    insertRollup(1L, BucketGranularity.MONTH, JUNE);

    execute();

    assertThat(hourKeys()).containsExactly(key(1L, JUNE));
  }

  @Test
  @DisplayName("Snapshot이 없는 잔여 HOUR 관측은 보존한다")
  void keepsHoursWithoutSnapshotKey() {
    insertHour(1L, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE);
    insertRollup(1L, BucketGranularity.MONTH, JUNE);
    jdbc.update(
        "DELETE FROM alcohol_popularity_snapshots WHERE bucket_granularity = 'HOUR' AND alcohol_id = 1");

    execute();

    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM alcohol_interest_observations WHERE bucket_granularity = 'HOUR'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("재실행해도 남은 원본과 상위 롤업이 그대로다")
  void rerunKeepsSameResult() {
    insertHour(1L, JUNE);
    insertHour(2L, JUNE);
    insertRollup(1L, BucketGranularity.WEEK, JUNE);
    insertRollup(1L, BucketGranularity.MONTH, JUNE);
    insertRollup(2L, BucketGranularity.WEEK, JUNE);

    execute();
    List<String> afterFirstRun = hourKeys();
    execute();

    assertThat(hourKeys()).isEqualTo(afterFirstRun).containsExactly(key(2L, JUNE));
    assertThat(rollupCount(BucketGranularity.WEEK)).isEqualTo(2);
    assertThat(rollupCount(BucketGranularity.MONTH)).isEqualTo(1);
  }

  @Test
  @DisplayName("대상 월 파라미터가 없으면 정리 기준을 정할 수 없어 실패한다")
  void failsWithoutTargetMonthParameter() {
    assertThatThrownBy(() -> execute(new JobParametersBuilder().toJobParameters()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("대상 월 파라미터");
  }

  private void execute() {
    execute(
        new JobParametersBuilder()
            .addString(PopularityRollupTasklet.GRANULARITY_PARAM, BucketGranularity.MONTH.name())
            .addLocalDateTime(ObservationBucket.BUCKET_AT_PARAM, TARGET_MONTH)
            .toJobParameters());
  }

  private void execute(JobParameters parameters) {
    JobExecution jobExecution =
        MetaDataInstanceFactory.createJobExecution(
            PopularityMonthlyRollupJobConfig.JOB_NAME, 1L, 1L, parameters);
    StepExecution stepExecution = jobExecution.createStepExecution("cleanup");
    tasklet.execute(
        stepExecution.createStepContribution(), new ChunkContext(new StepContext(stepExecution)));
  }

  private void insertHour(long alcoholId, LocalDateTime bucketAt) {
    insertRollup(alcoholId, BucketGranularity.HOUR, bucketAt);
  }

  private void insertRollup(
      long alcoholId, BucketGranularity granularity, LocalDateTime bucketAt) {
    for (String table : HourObservationCleanupTasklet.TABLES) {
      jdbc.update(
          "INSERT INTO "
              + table
              + " (alcohol_id, bucket_granularity, bucket_at, observed_at) VALUES (?, ?, ?, ?)",
          alcoholId,
          granularity.name(),
          bucketAt,
          bucketAt);
    }
  }

  /** 다섯 영역이 같은 키 집합을 갖는지 확인하고 그 키를 돌려준다. */
  private List<String> hourKeys() {
    List<String> keys = hourKeysOf(HourObservationCleanupTasklet.TABLES.get(0));
    for (String table : HourObservationCleanupTasklet.TABLES) {
      assertThat(hourKeysOf(table))
          .as("정리는 다섯 영역에 동일하게 적용된다: %s", table)
          .isEqualTo(keys);
    }
    return keys;
  }

  private List<String> hourKeysOf(String table) {
    return jdbc.query(
        "SELECT alcohol_id, bucket_at FROM "
            + table
            + " WHERE bucket_granularity = 'HOUR' ORDER BY bucket_at, alcohol_id",
        (rs, rowNumber) ->
            key(rs.getLong("alcohol_id"), rs.getObject("bucket_at", LocalDateTime.class)));
  }

  private String key(long alcoholId, LocalDateTime bucketAt) {
    return alcoholId + "@" + bucketAt;
  }

  private int rollupCount(BucketGranularity granularity) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM alcohol_interest_observations WHERE bucket_granularity = ?",
        Integer.class,
        granularity.name());
  }
}
