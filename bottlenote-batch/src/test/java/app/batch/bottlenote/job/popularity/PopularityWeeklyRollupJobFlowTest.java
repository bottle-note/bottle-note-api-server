package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import app.batch.bottlenote.job.popularity.PopularityWeeklyRollupJobConfig.PopularityWeeklyRollupQuartzJob;
import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
@DisplayName("[batch] 인기도 WEEK 롤업 Job 흐름")
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = PopularityWeeklyRollupJobFlowTest.TestConfig.class)
@TestPropertySource(
    properties = {
      "spring.batch.job.enabled=false",
      "spring.batch.jdbc.initialize-schema=always"
    })
class PopularityWeeklyRollupJobFlowTest {

  private static final LocalDateTime WEEK = LocalDateTime.of(2026, 8, 17, 0, 0);
  private static final List<String> EXECUTED = new ArrayList<>();
  private static final AtomicInteger ACTIVE = new AtomicInteger();
  private static final AtomicInteger MAX_ACTIVE = new AtomicInteger();
  private static volatile boolean failRollup;

  @Autowired private JobLauncher jobLauncher;
  @Autowired private Job popularityWeeklyRollupJob;

  @Test
  @DisplayName("네 축 롤업 성공 뒤 WEEK Snapshot을 단일 순차 흐름으로 실행한다")
  void rollupSucceeds_thenSnapshotRunsSequentially() throws Exception {
    failRollup = false;

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(EXECUTED).containsExactly("rollup", "snapshot");
    assertThat(MAX_ACTIVE).hasValue(1);
  }

  @Test
  @DisplayName("네 축 롤업이 실패하면 WEEK Snapshot을 만들지 않는다")
  void rollupFails_thenSnapshotDoesNotRun() throws Exception {
    failRollup = true;

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(EXECUTED).containsExactly("rollup");
  }

  @Test
  @DisplayName("주간 Quartz는 월요일 02시 50분에 별도 identity로 등록된다")
  void registersWeeklyQuartzBinding() {
    PopularityQuartzConfig config = new PopularityQuartzConfig();

    JobDetail detail = config.popularityWeeklyRollupJobDetail();
    CronTrigger trigger = (CronTrigger) config.popularityWeeklyRollupJobTrigger();

    assertThat(detail.getKey().getName()).isEqualTo("popularityWeeklyRollupJob");
    assertThat(trigger.getKey().getName()).isEqualTo("popularityWeeklyRollupTrigger");
    assertThat(trigger.getJobKey()).isEqualTo(detail.getKey());
    assertThat(trigger.getCronExpression()).isEqualTo("0 50 2 ? * MON");
  }

  @Test
  @DisplayName("주간 Quartz는 실행 시점에 닫힌 직전 월요일 버킷만 선택한다")
  void selectsPreviousClosedWeek() {
    LocalDateTime executionTime = LocalDateTime.of(2026, 8, 24, 2, 50);
    JobParametersBuilder builder = new JobParametersBuilder();
    PopularityWeeklyRollupQuartzJob quartzJob = new PopularityWeeklyRollupQuartzJob(null, null);

    quartzJob.customizeJobParameters(builder, executionTime);

    JobParameters parameters = builder.toJobParameters();
    assertThat(parameters.getString(PopularityRollupTasklet.GRANULARITY_PARAM)).isEqualTo("WEEK");
    assertThat(parameters.getLocalDateTime(ObservationBucket.BUCKET_AT_PARAM))
        .isEqualTo(LocalDateTime.of(2026, 8, 17, 0, 0));
    assertThat(PopularityWeeklyRollupQuartzJob.closedWeekAt(executionTime.minusDays(1)))
        .isEqualTo(LocalDateTime.of(2026, 8, 10, 0, 0));
  }

  private JobExecution run() throws Exception {
    EXECUTED.clear();
    ACTIVE.set(0);
    MAX_ACTIVE.set(0);
    JobParameters parameters =
        new JobParametersBuilder()
            .addString(PopularityRollupTasklet.GRANULARITY_PARAM, BucketGranularity.WEEK.name())
            .addLocalDateTime(ObservationBucket.BUCKET_AT_PARAM, WEEK)
            .addString("run", String.valueOf(System.nanoTime()))
            .toJobParameters();
    return jobLauncher.run(popularityWeeklyRollupJob, parameters);
  }

  private static RepeatStatus record(String stage) {
    int active = ACTIVE.incrementAndGet();
    MAX_ACTIVE.accumulateAndGet(active, Math::max);
    try {
      EXECUTED.add(stage);
      if (failRollup && stage.equals("rollup")) {
        throw new IllegalStateException("WEEK 롤업을 의도적으로 실패시킨다");
      }
      return RepeatStatus.FINISHED;
    } finally {
      ACTIVE.decrementAndGet();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(PopularityWeeklyRollupJobConfig.class)
  static class TestConfig {

    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
          .setType(EmbeddedDatabaseType.H2)
          .setName("popularity-weekly-flow-" + System.nanoTime())
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
  }
}
