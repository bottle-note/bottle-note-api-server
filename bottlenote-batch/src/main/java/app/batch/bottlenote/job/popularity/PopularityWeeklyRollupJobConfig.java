package app.batch.bottlenote.job.popularity;

import app.batch.bottlenote.BatchQuartzJob;
import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/** 닫힌 HOUR 관측을 WEEK로 롤업하고 주간 Snapshot을 만드는 Job. */
@Configuration
@RequiredArgsConstructor
public class PopularityWeeklyRollupJobConfig {

  public static final String JOB_NAME = "popularityWeeklyRollupJob";

  private final PopularityRollupTasklet popularityRollupTasklet;
  private final PopularitySnapshotTasklet popularitySnapshotTasklet;

  @Bean
  public Job popularityWeeklyRollupJob(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(popularityWeeklyRollupStep(jobRepository, transactionManager))
        .next(popularityWeeklySnapshotStep(jobRepository, transactionManager))
        .build();
  }

  /** 네 축을 한 트랜잭션에서 순차 처리해 DB worker connection을 하나만 사용한다. */
  @Bean
  public Step popularityWeeklyRollupStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("popularityWeeklyRollupStep", jobRepository)
        .tasklet(popularityRollupTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step popularityWeeklySnapshotStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("popularityWeeklySnapshotStep", jobRepository)
        .tasklet(popularitySnapshotTasklet, transactionManager)
        .build();
  }

  @Component
  @DisallowConcurrentExecution
  public static class PopularityWeeklyRollupQuartzJob extends BatchQuartzJob {

    public PopularityWeeklyRollupQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
      super(jobLauncher, jobRegistry, JOB_NAME, JOB_NAME);
    }

    @Override
    protected void customizeJobParameters(
        JobParametersBuilder jobParameters, LocalDateTime executionTime) {
      jobParameters.addString(
          PopularityRollupTasklet.GRANULARITY_PARAM,
          BucketGranularity.WEEK.name(),
          false);
      jobParameters.addLocalDateTime(
          ObservationBucket.BUCKET_AT_PARAM, closedWeekAt(executionTime), false);
    }

    static LocalDateTime closedWeekAt(LocalDateTime executionTime) {
      return BucketGranularity.WEEK.startAt(executionTime).minusWeeks(1);
    }
  }
}
