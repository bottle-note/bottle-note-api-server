package app.batch.bottlenote;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;

@Tag("batch")
@DisplayName("[batch] Quartz Spring Batch 공통 Wrapper")
class BatchQuartzJobTest {

  @Test
  @DisplayName("Spring Batch Job이 성공하면 Quartz Job도 정상 종료한다")
  void executeInternal_whenBatchJobCompletes_finishesNormally() {
    TestBatchQuartzJob job = new TestBatchQuartzJob(jobLauncher(BatchStatus.COMPLETED));

    assertThatCode(() -> job.executeInternal(null)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Spring Batch Job이 FAILED면 Quartz JobExecutionException을 던진다")
  void executeInternal_whenBatchJobFails_throwsJobExecutionException() {
    TestBatchQuartzJob job = new TestBatchQuartzJob(jobLauncher(BatchStatus.FAILED));

    assertThatThrownBy(() -> job.executeInternal(null))
        .isInstanceOf(JobExecutionException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Spring Batch job testBatchJob ended with status FAILED");
  }

  @Test
  @DisplayName("Spring Batch Job이 정상 완료 외 상태면 Quartz JobExecutionException을 던진다")
  void executeInternal_whenBatchJobStops_throwsJobExecutionException() {
    TestBatchQuartzJob job = new TestBatchQuartzJob(jobLauncher(BatchStatus.STOPPED));

    assertThatThrownBy(() -> job.executeInternal(null))
        .isInstanceOf(JobExecutionException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Spring Batch job testBatchJob ended with status STOPPED");
  }

  private static JobLauncher jobLauncher(BatchStatus status) {
    return (job, parameters) -> {
      JobExecution execution = new JobExecution(1L);
      execution.setStatus(status);
      return execution;
    };
  }

  private static class TestBatchQuartzJob extends BatchQuartzJob {

    TestBatchQuartzJob(JobLauncher jobLauncher) {
      super(jobLauncher, jobRegistry(), "testBatchJob", "testScheduler");
    }

    private static JobRegistry jobRegistry() {
      return new JobRegistry() {
        @Override
        public void register(JobFactory jobFactory) {}

        @Override
        public void unregister(String jobName) {}

        @Override
        public Collection<String> getJobNames() {
          return List.of("testBatchJob");
        }

        @Override
        public Job getJob(String jobName) {
          return new Job() {
            @Override
            public String getName() {
              return "testBatchJob";
            }

            @Override
            public void execute(JobExecution execution) {}
          };
        }
      };
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
      super.executeInternal(context);
    }
  }
}
