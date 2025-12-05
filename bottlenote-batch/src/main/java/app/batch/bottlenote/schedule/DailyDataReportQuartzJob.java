package app.batch.bottlenote.schedule;

import static app.batch.bottlenote.job.DailyDataReportJobConfig.DAILY_DATA_REPORT_JOB_NAME;
import static java.time.LocalDateTime.now;

import app.external.version.config.AppInfoConfig;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 일일 데이터 리포트를 위한 Quartz Job 구현 클래스입니다.
 *
 * <p>이 클래스는 BatchQuartzJob을 상속받아 일일 데이터 리포트 배치 작업에 특화된 Quartz Job을 구현합니다. Spring의 @Component 어노테이션을
 * 통해 스프링 빈으로 등록되며, QuartzConfig에서 이 클래스를 사용하여 JobDetail과 Trigger를 설정합니다.
 *
 * <p>dev 브랜치에서는 실행되지 않습니다.
 */
@Slf4j
@Component
public class DailyDataReportQuartzJob extends BatchQuartzJob {

  private static final String DEV_BRANCH = "dev";
  private final AppInfoConfig appInfoConfig;

  /**
   * 일일 데이터 리포트 Quartz Job 생성자입니다.
   *
   * @param jobLauncher Spring Batch Job 실행을 위한 런처
   * @param jobRegistry Spring Batch Job 레지스트리
   * @param appInfoConfig 앱 정보 설정
   */
  public DailyDataReportQuartzJob(
      JobLauncher jobLauncher, JobRegistry jobRegistry, AppInfoConfig appInfoConfig) {
    super(jobLauncher, jobRegistry, DAILY_DATA_REPORT_JOB_NAME, "dailyDataReportJob");
    this.appInfoConfig = appInfoConfig;
  }

  @Override
  protected void executeInternal(@NonNull JobExecutionContext context)
      throws JobExecutionException {
    String gitBranch = Objects.requireNonNullElse(appInfoConfig.getGitBranch(), "unknown");
    if (DEV_BRANCH.equals(gitBranch)) {
      log.info("[BATCH SKIP] dailyDataReportJob skipped on dev branch at: {}", now());
      return;
    }
    super.executeInternal(context);
  }
}
