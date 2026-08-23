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

/**
 * 닫힌 달력 월을 HOUR 원본에서 직접 롤업하고, Snapshot 성공 뒤에만 보관 기간이 지난 HOUR를 정리하는 Job.
 *
 * <p>WEEK 결과를 합치지 않는다. 한 주가 두 달에 걸치면 그 주의 값을 두 달로 정확히 나눌 수 없어 달력 기준 월값이 어긋난다.
 *
 * <p>정리를 별도 Job으로 두지 않는다. 월간 롤업이 성공해야 해당 기간의 HOUR를 지워도 안전하므로, 같은 Job의 마지막 Step이 맡는 것이 성공 판정과 정리를 한
 * 실행 안에 묶는다.
 */
@Configuration
@RequiredArgsConstructor
public class PopularityMonthlyRollupJobConfig {

  public static final String JOB_NAME = "popularityMonthlyRollupJob";

  private final PopularityRollupTasklet popularityRollupTasklet;
  private final PopularitySnapshotTasklet popularitySnapshotTasklet;
  private final HourObservationCleanupTasklet hourObservationCleanupTasklet;

  @Bean
  public Job popularityMonthlyRollupJob(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(popularityMonthlyRollupStep(jobRepository, transactionManager))
        .next(popularityMonthlySnapshotStep(jobRepository, transactionManager))
        .next(hourObservationCleanupStep(jobRepository, transactionManager))
        .build();
  }

  /** 네 축을 한 트랜잭션에서 순차 처리해 DB worker connection을 하나만 사용한다. */
  @Bean
  public Step popularityMonthlyRollupStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("popularityMonthlyRollupStep", jobRepository)
        .tasklet(popularityRollupTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step popularityMonthlySnapshotStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("popularityMonthlySnapshotStep", jobRepository)
        .tasklet(popularitySnapshotTasklet, transactionManager)
        .build();
  }

  /** 앞선 Step이 실패하면 도달하지 않는다. 실제 삭제는 내부 REQUIRES_NEW chunk 트랜잭션으로 나뉜다. */
  @Bean
  public Step hourObservationCleanupStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("hourObservationCleanupStep", jobRepository)
        .tasklet(hourObservationCleanupTasklet, transactionManager)
        .build();
  }

  @Component
  @DisallowConcurrentExecution
  public static class PopularityMonthlyRollupQuartzJob extends BatchQuartzJob {

    public PopularityMonthlyRollupQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
      super(jobLauncher, jobRegistry, JOB_NAME, JOB_NAME);
    }

    @Override
    protected void customizeJobParameters(
        JobParametersBuilder jobParameters, LocalDateTime executionTime) {
      jobParameters.addString(
          PopularityRollupTasklet.GRANULARITY_PARAM, BucketGranularity.MONTH.name(), false);
      jobParameters.addLocalDateTime(
          ObservationBucket.BUCKET_AT_PARAM, closedMonthAt(executionTime), false);
    }

    static LocalDateTime closedMonthAt(LocalDateTime executionTime) {
      return BucketGranularity.MONTH.startAt(executionTime).minusMonths(1);
    }
  }
}
