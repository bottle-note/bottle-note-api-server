package app.batch.bottlenote.job.ranking;

import app.batch.bottlenote.BatchQuartzJob;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
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
 * 베스트 리뷰 선정 배치 Job.
 *
 * <p>단일 Tasklet Step이다. 선정 집합 계산, is_best 갱신, 변동 로그 적재가 한 트랜잭션 안에서 끝난다.
 */
@Configuration
@RequiredArgsConstructor
public class BestReviewSelectionJobConfig {

  public static final String BEST_REVIEW_JOB_NAME = "bestReviewSelectedJob";
  public static final String BEST_REVIEW_STEP_NAME = "bestReviewSelectionStep";

  private final BestReviewSelectionTasklet bestReviewSelectionTasklet;

  @Bean
  public Job bestReviewSelectedJob(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    Step selectionStep =
        new StepBuilder(BEST_REVIEW_STEP_NAME, jobRepository)
            .tasklet(bestReviewSelectionTasklet, transactionManager)
            .build();
    return new JobBuilder(BEST_REVIEW_JOB_NAME, jobRepository).start(selectionStep).build();
  }

  @Component
  public static class BestReviewQuartzJob extends BatchQuartzJob {
    public BestReviewQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
      super(jobLauncher, jobRegistry, BEST_REVIEW_JOB_NAME, "bestReviewSelectedJob");
    }
  }
}
