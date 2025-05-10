package app.batch.bottlenote.schedule;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static java.time.LocalDateTime.now;


/**
 * Quartz 스케줄러를 이용한 Spring Batch Job 실행을 위한 추상 기본 클래스입니다.
 * <p>
 * 이 클래스는 Spring의 QuartzJobBean을 상속받아 Quartz Job을 구현합니다.
 * Spring Batch Job을 실행하기 위한 공통 로직을 포함하고 있어 코드 중복을 줄이고
 * 일관된 방식으로 배치 작업을 실행할 수 있게 합니다.
 * <p>
 * Quartz는 스케줄링을 전담하고, 실제 비즈니스 로직은 Spring Batch가 담당하여 
 * 관심사를 명확히 분리합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BatchQuartzJob extends QuartzJobBean {

	private final JobLauncher jobLauncher;
	private final JobRegistry jobRegistry;
	private final String batchJobName;
	private final String schedulerName;

	/**
	 * Quartz 스케줄러에 의해 호출되는 Job 실행 메소드입니다.
	 * <p>
	 * 이 메소드는 다음과 같은 작업을 수행합니다:
	 * 1. 배치 작업 시작 로그 기록
	 * 2. Spring Batch Job Registry에서 Job 객체 조회
	 * 3. JobParameters 생성 (실행 시간과 작업 이름 포함)
	 * 4. JobLauncher를 통해 Spring Batch Job 실행
	 * 5. 작업 결과에 따라 성공/실패 로그 기록
	 * <p>
	 * 오류 발생시 JobExecutionException을 던져 Quartz에게 작업 실패를 알립니다.
	 *
	 * @param context Quartz에서 제공하는 작업 실행 컨텍스트 (작업 실행 상태 및 데이터 포함)
	 * @throws JobExecutionException 작업 실행 중 오류 발생 시
	 */
	@Override
	protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
		log.info("[BATCH START] {} scheduler started at: {}", schedulerName, now());

		try {
			Job job = jobRegistry.getJob(batchJobName);
			JobParametersBuilder jobParam =
					new JobParametersBuilder()
							.addLocalDateTime("localDateTime", now())
							.addString("jobName", schedulerName);

			jobLauncher.run(job, jobParam.toJobParameters());
			log.info("[BATCH SUCCESS] {} scheduler completed successfully at: {}", schedulerName, now());
		} catch (Exception e) {
			log.error("[BATCH FAILURE] {} scheduler failed: {}", schedulerName, e.getMessage());
			throw new JobExecutionException(e);
		}
	}
}
