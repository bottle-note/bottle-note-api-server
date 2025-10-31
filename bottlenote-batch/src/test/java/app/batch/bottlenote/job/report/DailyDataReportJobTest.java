package app.batch.bottlenote.job.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.support.report.service.DailyDataReportService;
import app.external.webhook.config.DiscordWebhookProperties;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

@Tag("integration")
@SpringBatchTest
@SpringBootTest
@ContextConfiguration(classes = {DailyDataReportJobTest.TestConfig.class, DailyDataReportJob.class})
@DisplayName("[integration] [batch] DailyDataReportJob")
class DailyDataReportJobTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@MockBean
	private DailyDataReportService dailyDataReportService;

	@MockBean
	private DiscordWebhookProperties discordWebhookProperties;

	@Test
	@DisplayName("일일 데이터 리포트 Job이 정상적으로 실행된다")
	void testDailyDataReportJob_정상실행() throws Exception {
		// given
		String webhookUrl = "https://discord.com/api/webhooks/test";
		when(discordWebhookProperties.getUrl()).thenReturn(webhookUrl);
		doNothing().when(dailyDataReportService).collectAndSendDailyReport(any(LocalDate.class), any(String.class));

		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("time", System.currentTimeMillis())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		verify(dailyDataReportService, times(1)).collectAndSendDailyReport(any(LocalDate.class), any(String.class));
	}

	@Test
	@DisplayName("Job 이름이 올바르게 설정되어 있다")
	void testDailyDataReportJob_Job이름검증() {
		// given & when & then
		assertEquals(DailyDataReportJob.DAILY_DATA_REPORT_JOB_NAME, "dailyDataReportJob");
	}

	@Test
	@DisplayName("Job이 하나의 Step으로 구성되어 있다")
	void testDailyDataReportJob_Step구성검증() throws Exception {
		// given
		String webhookUrl = "https://discord.com/api/webhooks/test";
		when(discordWebhookProperties.getUrl()).thenReturn(webhookUrl);
		doNothing().when(dailyDataReportService).collectAndSendDailyReport(any(LocalDate.class), any(String.class));

		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("time", System.currentTimeMillis())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals("collectAndSendDailyDataStep", jobExecution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	@DisplayName("전날 데이터를 수집한다")
	void testDailyDataReportJob_전날데이터수집검증() throws Exception {
		// given
		String webhookUrl = "https://discord.com/api/webhooks/test";
		when(discordWebhookProperties.getUrl()).thenReturn(webhookUrl);
		doNothing().when(dailyDataReportService).collectAndSendDailyReport(any(LocalDate.class), any(String.class));

		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("time", System.currentTimeMillis())
			.toJobParameters();

		LocalDate expectedDate = LocalDate.now().minusDays(1);

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		verify(dailyDataReportService, times(1)).collectAndSendDailyReport(expectedDate, webhookUrl);
	}

	@Configuration
	static class TestConfig {

		@Bean
		public JobLauncherTestUtils jobLauncherTestUtils(
			Job dailyDataReportJob,
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager
		) {
			JobLauncherTestUtils utils = new JobLauncherTestUtils();
			utils.setJob(dailyDataReportJob);
			return utils;
		}
	}
}
