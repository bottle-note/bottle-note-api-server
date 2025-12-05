package app.batch.bottlenote.schedule;


import static app.batch.bottlenote.job.BestReviewSelectionJobConfig.BEST_REVIEW_JOB_NAME;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * 베스트 리뷰 선정을 위한 Quartz Job 구현 클래스입니다.
 * <p>
 * 이 클래스는 BatchQuartzJob을 상속받아 베스트 리뷰 선정 배치 작업에 특화된
 * Quartz Job을 구현합니다. Spring의 @Component 어노테이션을 통해 스프링 빈으로 등록되며,
 * QuartzConfig에서 이 클래스를 사용하여 JobDetail과 Trigger를 설정합니다.
 */
@Component
public class BestReviewQuartzJob extends BatchQuartzJob {

	/**
	 * 베스트 리뷰 선정 Quartz Job 생성자입니다.
	 * <p>
	 * 상위 클래스에 필요한 파라미터를 전달합니다:
	 * - jobLauncher: Spring Batch Job을 실행하기 위한 런처
	 * - jobRegistry: Spring Batch Job을 저장하고 관리하는 레지스트리
	 * - 베스트 리뷰 선정 작업 이름(BEST_REVIEW_JOB_NAME)
	 * - 스케줄러 식별 이름("bestReviewSelectedJob")
	 *
	 * @param jobLauncher Spring Batch Job 실행을 위한 런처
	 * @param jobRegistry Spring Batch Job 레지스트리
	 */
	public BestReviewQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
		super(jobLauncher, jobRegistry, BEST_REVIEW_JOB_NAME, "bestReviewSelectedJob");
	}
}
