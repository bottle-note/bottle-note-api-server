package app.batch.bottlenote.job.popularity;

import app.batch.bottlenote.BatchQuartzJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 인기도 관측 Job.
 *
 * <p>네 축을 병렬로 관측한 뒤 합류해서 최종 인기도를 적재한다. 축끼리 의존이 없으므로 병렬이 가능하고, 한 축이 무거워져도 전체 소요가 넷의 합이 되지 않는다.
 *
 * <p>split 안의 flow가 하나라도 실패하면 전체 flow가 실패로 끝나 뒤따르는 적재 Step은 실행되지 않는다. 이때 성공한 축의 관측 행은 남고, 최종 테이블만
 * 갱신되지 않아 조회가 직전 버킷을 계속 본다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PopularityObservationJobConfig {

  public static final String JOB_NAME = "popularityObservationJob";

  private final InterestObservationTasklet interestObservationTasklet;
  private final RatingObservationTasklet ratingObservationTasklet;
  private final PickObservationTasklet pickObservationTasklet;
  private final EngagementObservationTasklet engagementObservationTasklet;
  private final PopularitySnapshotTasklet popularitySnapshotTasklet;

  @Bean
  public Job popularityObservationJob(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {

    Flow parallelObservation =
        new FlowBuilder<Flow>("popularityParallelObservation")
            .split(popularityObservationTaskExecutor())
            .add(
                flowOf("interestObservationFlow", interestStep(jobRepository, transactionManager)),
                flowOf("ratingObservationFlow", ratingStep(jobRepository, transactionManager)),
                flowOf("pickObservationFlow", pickStep(jobRepository, transactionManager)),
                flowOf(
                    "engagementObservationFlow",
                    engagementStep(jobRepository, transactionManager)))
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
        .start(parallelObservation)
        .next(popularitySnapshotStep(jobRepository, transactionManager))
        .end()
        .build();
  }

  @Bean
  public TaskExecutor popularityObservationTaskExecutor() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("popularity-obs-");
    // 축이 넷이므로 그 이상 동시에 뜰 일이 없다
    executor.setConcurrencyLimit(4);
    return executor;
  }

  private Flow flowOf(String name, Step step) {
    return new FlowBuilder<Flow>(name).start(step).build();
  }

  @Bean
  public Step interestStep(JobRepository jobRepository, PlatformTransactionManager tx) {
    return tasklet("interestObservationStep", interestObservationTasklet, jobRepository, tx);
  }

  @Bean
  public Step ratingStep(JobRepository jobRepository, PlatformTransactionManager tx) {
    return tasklet("ratingObservationStep", ratingObservationTasklet, jobRepository, tx);
  }

  @Bean
  public Step pickStep(JobRepository jobRepository, PlatformTransactionManager tx) {
    return tasklet("pickObservationStep", pickObservationTasklet, jobRepository, tx);
  }

  @Bean
  public Step engagementStep(JobRepository jobRepository, PlatformTransactionManager tx) {
    return tasklet("engagementObservationStep", engagementObservationTasklet, jobRepository, tx);
  }

  @Bean
  public Step popularitySnapshotStep(JobRepository jobRepository, PlatformTransactionManager tx) {
    return tasklet("popularitySnapshotStep", popularitySnapshotTasklet, jobRepository, tx);
  }

  private Step tasklet(
      String name, Tasklet target, JobRepository jobRepository, PlatformTransactionManager tx) {
    return new StepBuilder(name, jobRepository).tasklet(target, tx).build();
  }

  @Component
  public static class PopularityObservationQuartzJob extends BatchQuartzJob {
    public PopularityObservationQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
      super(jobLauncher, jobRegistry, JOB_NAME, JOB_NAME);
    }
  }
}
