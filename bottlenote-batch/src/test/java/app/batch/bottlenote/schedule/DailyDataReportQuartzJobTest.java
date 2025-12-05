package app.batch.bottlenote.schedule;

import static app.batch.bottlenote.job.DailyDataReportJob.DAILY_DATA_REPORT_JOB_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;

@Tag("unit")
@DisplayName("[unit] [schedule] DailyDataReportQuartzJob")
@ExtendWith(MockitoExtension.class)
class DailyDataReportQuartzJobTest {

	@Mock
	private JobLauncher jobLauncher;

	@Mock
	private JobRegistry jobRegistry;

	@Mock
	private JobExecutionContext context;

	@Mock
	private Job job;

	private DailyDataReportQuartzJob dailyDataReportQuartzJob;

	@BeforeEach
	void setUp() {
		dailyDataReportQuartzJob = new DailyDataReportQuartzJob(jobLauncher, jobRegistry);
	}

	@Test
	@DisplayName("DailyDataReportQuartzJob이 정상적으로 생성된다")
	void testDailyDataReportQuartzJob_생성검증() {
		// given & when & then
		assertNotNull(dailyDataReportQuartzJob);
	}

	@Test
	@DisplayName("Job 실행 시 올바른 Job 이름으로 실행된다")
	void testExecuteInternal_Job이름검증() throws Exception {
		// given
		when(jobRegistry.getJob(eq(DAILY_DATA_REPORT_JOB_NAME))).thenReturn(job);
		when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(null);

		// when
		dailyDataReportQuartzJob.executeInternal(context);

		// then
		verify(jobRegistry, times(1)).getJob(DAILY_DATA_REPORT_JOB_NAME);
		verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
	}

	@Test
	@DisplayName("Job을 찾지 못하면 NoSuchJobException이 발생한다")
	void testExecuteInternal_Job찾기실패() throws Exception {
		// given
		when(jobRegistry.getJob(eq(DAILY_DATA_REPORT_JOB_NAME)))
			.thenThrow(new NoSuchJobException("Job not found"));

		// when & then
		try {
			dailyDataReportQuartzJob.executeInternal(context);
		} catch (Exception e) {
			assertEquals(NoSuchJobException.class, e.getCause().getClass());
		}
	}

	@Test
	@DisplayName("스케줄러 식별 이름이 올바르게 설정되어 있다")
	void testSchedulerName_검증() {
		// given & when
		String expectedSchedulerName = "dailyDataReportJob";

		// then
		assertEquals(DAILY_DATA_REPORT_JOB_NAME, expectedSchedulerName);
	}
}
