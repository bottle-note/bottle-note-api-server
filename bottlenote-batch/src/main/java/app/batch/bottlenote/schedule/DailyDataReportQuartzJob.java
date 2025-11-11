package app.batch.bottlenote.schedule;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import static app.batch.bottlenote.job.report.DailyDataReportJob.DAILY_DATA_REPORT_JOB_NAME;

/**
 * 일일 데이터 리포트를 위한 Quartz Job 구현 클래스입니다.
 * <p>
 * 이 클래스는 BatchQuartzJob을 상속받아 일일 데이터 리포트 배치 작업에 특화된
 * Quartz Job을 구현합니다. Spring의 @Component 어노테이션을 통해 스프링 빈으로 등록되며,
 * QuartzConfig에서 이 클래스를 사용하여 JobDetail과 Trigger를 설정합니다.
 */
@Component
public class DailyDataReportQuartzJob extends BatchQuartzJob {

	/**
	 * 일일 데이터 리포트 Quartz Job 생성자입니다.
	 * <p>
	 * 상위 클래스에 필요한 파라미터를 전달합니다:
	 * - jobLauncher: Spring Batch Job을 실행하기 위한 런처
	 * - jobRegistry: Spring Batch Job을 저장하고 관리하는 레지스트리
	 * - 일일 데이터 리포트 작업 이름(DAILY_DATA_REPORT_JOB_NAME)
	 * - 스케줄러 식별 이름("dailyDataReportJob")
	 *
	 * @param jobLauncher Spring Batch Job 실행을 위한 런처
	 * @param jobRegistry Spring Batch Job 레지스트리
	 */
	public DailyDataReportQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
		super(jobLauncher, jobRegistry, DAILY_DATA_REPORT_JOB_NAME, "dailyDataReportJob");
	}
}
