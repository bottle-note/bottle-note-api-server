package app.batch.bottlenote.job.report;

import app.bottlenote.support.report.service.DailyDataReportService;
import app.external.webhook.config.DiscordWebhookProperties;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 일일 데이터 리포트 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyDataReportJob {
	public static final String DAILY_DATA_REPORT_JOB_NAME = "dailyDataReportJob";

	private final DailyDataReportService dailyDataReportService;
	private final DiscordWebhookProperties discordWebhookProperties;

	/**
	 * 일일 데이터 리포트 Job을 생성합니다.
	 */
	@Bean
	public Job dailyDataReportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		log.debug("일일 데이터 리포트 Job 초기화 시작");

		Step collectAndSendStep = getCollectAndSendStep(jobRepository, transactionManager);

		log.debug("일일 데이터 리포트 Job 초기화 완료");
		return new JobBuilder(DAILY_DATA_REPORT_JOB_NAME, jobRepository)
			.start(collectAndSendStep)
			.build();
	}

	/**
	 * 데이터를 수집하고 웹훅으로 전송하는 스텝을 생성합니다.
	 */
	private Step getCollectAndSendStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("collectAndSendDailyDataStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				LocalDate targetDate = LocalDate.now().minusDays(1);
				log.info("일일 데이터 리포트 배치 작업 시작: {}", targetDate);

				String webhookUrl = discordWebhookProperties.getUrl();
				dailyDataReportService.collectAndSendDailyReport(targetDate, webhookUrl);

				log.info("일일 데이터 리포트 배치 작업 완료: {}", targetDate);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}
}
