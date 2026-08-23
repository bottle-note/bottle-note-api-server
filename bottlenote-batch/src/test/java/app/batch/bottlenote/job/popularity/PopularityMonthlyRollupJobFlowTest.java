package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import app.batch.bottlenote.job.popularity.PopularityMonthlyRollupJobConfig.PopularityMonthlyRollupQuartzJob;
import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;

@Tag("batch")
@DisplayName("[batch] 인기도 MONTH 롤업 Job 흐름")
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = PopularityMonthlyRollupJobFlowTest.TestConfig.class)
@TestPropertySource(
    properties = {
      "spring.batch.job.enabled=false",
      "spring.batch.jdbc.initialize-schema=always"
    })
class PopularityMonthlyRollupJobFlowTest {

  private static final LocalDateTime MONTH = LocalDateTime.of(2026, 7, 1, 0, 0);
  private static final List<String> EXECUTED = new ArrayList<>();
  private static volatile String failingStage;

  @Autowired private JobLauncher jobLauncher;
  @Autowired private Job popularityMonthlyRollupJob;

  @Test
  @DisplayName("롤업과 Snapshot이 성공한 뒤에만 HOUR 정리를 실행한다")
  void cleansUpOnlyAfterSnapshotSucceeds() throws Exception {
    failingStage = null;

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(EXECUTED).containsExactly("rollup", "snapshot", "cleanup");
  }

  @Test
  @DisplayName("Snapshot이 실패하면 HOUR 정리를 실행하지 않는다")
  void snapshotFails_thenCleanupDoesNotRun() throws Exception {
    failingStage = "snapshot";

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(EXECUTED).containsExactly("rollup", "snapshot");
  }

  @Test
  @DisplayName("롤업이 실패하면 Snapshot도 정리도 실행하지 않는다")
  void rollupFails_thenNothingElseRuns() throws Exception {
    failingStage = "rollup";

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(EXECUTED).containsExactly("rollup");
  }

  @Test
  @DisplayName("월간 Quartz는 매월 1일 03시 50분에 별도 identity로 등록된다")
  void registersMonthlyQuartzBinding() {
    PopularityQuartzConfig config = new PopularityQuartzConfig();

    JobDetail detail = config.popularityMonthlyRollupJobDetail();
    CronTrigger trigger = (CronTrigger) config.popularityMonthlyRollupJobTrigger();

    assertThat(detail.getKey().getName()).isEqualTo("popularityMonthlyRollupJob");
    assertThat(trigger.getKey().getName()).isEqualTo("popularityMonthlyRollupTrigger");
    assertThat(trigger.getJobKey()).isEqualTo(detail.getKey());
    assertThat(trigger.getCronExpression()).isEqualTo("0 50 3 1 * ?");
  }

  @Test
  @DisplayName("월간 Quartz는 WEEK를 거치지 않고 닫힌 직전 달력 월을 선택한다")
  void selectsPreviousClosedCalendarMonth() {
    LocalDateTime executionTime = LocalDateTime.of(2026, 8, 1, 3, 50);
    JobParametersBuilder builder = new JobParametersBuilder();
    PopularityMonthlyRollupQuartzJob quartzJob = new PopularityMonthlyRollupQuartzJob(null, null);

    quartzJob.customizeJobParameters(builder, executionTime);

    JobParameters parameters = builder.toJobParameters();
    assertThat(parameters.getString(PopularityRollupTasklet.GRANULARITY_PARAM)).isEqualTo("MONTH");
    assertThat(parameters.getLocalDateTime(ObservationBucket.BUCKET_AT_PARAM))
        .isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
    assertThat(PopularityMonthlyRollupQuartzJob.closedMonthAt(LocalDateTime.of(2026, 3, 1, 3, 50)))
        .isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
  }

  private JobExecution run() throws Exception {
    EXECUTED.clear();
    JobParameters parameters =
        new JobParametersBuilder()
            .addString(PopularityRollupTasklet.GRANULARITY_PARAM, BucketGranularity.MONTH.name())
            .addLocalDateTime(ObservationBucket.BUCKET_AT_PARAM, MONTH)
            .addString("run", String.valueOf(System.nanoTime()))
            .toJobParameters();
    return jobLauncher.run(popularityMonthlyRollupJob, parameters);
  }

  private static RepeatStatus record(String stage) {
    EXECUTED.add(stage);
    if (stage.equals(failingStage)) {
      throw new IllegalStateException("MONTH " + stage + "을 의도적으로 실패시킨다");
    }
    return RepeatStatus.FINISHED;
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(PopularityMonthlyRollupJobConfig.class)
  static class TestConfig {

    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
          .setType(EmbeddedDatabaseType.H2)
          .setName("popularity-monthly-flow-" + System.nanoTime())
          .build();
    }

    @Bean
    public PopularityRollupTasklet popularityRollupTasklet() {
      return new PopularityRollupTasklet(null) {
        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
          return record("rollup");
        }
      };
    }

    @Bean
    public PopularitySnapshotTasklet popularitySnapshotTasklet() {
      return new PopularitySnapshotTasklet(null, new PopularityObservationProperties()) {
        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
          return record("snapshot");
        }
      };
    }

    @Bean
    public HourObservationCleanupTasklet hourObservationCleanupTasklet() {
      return new HourObservationCleanupTasklet(
          null, new PopularityObservationProperties(), new ResourcelessTransactionManager()) {
        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
          return record("cleanup");
        }
      };
    }
  }
}
