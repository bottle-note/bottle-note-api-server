package app.batch.bottlenote.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static app.batch.bottlenote.job.BestReviewSelectionJob.BEST_REVIEW_JOB_NAME;
import static app.batch.bottlenote.job.PopularAlcoholSelectionJob.POPULAR_JOB_NAME;
import static java.time.LocalDateTime.now;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchSchedules {

	private final JobLauncher jobLauncher;
	private final JobRegistry jobRegistry;

	@Scheduled(cron = "0 0 0 * * *")
	public void bestReviewSelectedJob() {
		final String jobName = "bestReviewSelectedJob";
		log.info("start scheduler {} : {}", jobName, now());

		try {
			Job job = jobRegistry.getJob(BEST_REVIEW_JOB_NAME); // job 이름
			JobParametersBuilder jobParam =
				new JobParametersBuilder()
					.addLocalDateTime("localDateTime", now())
					.addString("jobName", jobName);
			jobLauncher.run(job, jobParam.toJobParameters());
		} catch (Exception e) {
			log.error("best review selected job error : {}", e.getMessage());
		}
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void popularSelectedJob() {
		final String jobName = "popularReviewSelectedJob";
		log.info("start scheduler {} : {}", jobName, now());
		try {
			Job job = jobRegistry.getJob(POPULAR_JOB_NAME); // job 이름
			JobParametersBuilder jobParam =
				new JobParametersBuilder()
					.addLocalDateTime("localDateTime", now())
					.addString("jobName", jobName);
			jobLauncher.run(job, jobParam.toJobParameters());
		} catch (Exception e) {
			log.error("popular review selected job error : {}", e.getMessage());
		}
	}
}
