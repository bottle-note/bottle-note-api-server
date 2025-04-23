package app.bottlenote.history.quartz;

import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ViewHistoryJobConfig {

	@Bean
	public Trigger viewHistorySyncJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(viewHistorySyncJobDetail())
				.withIdentity("viewHistorySyncTrigger")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 50/10 * * ?"))
				.build();
	}

	@Bean
	public JobDetail viewHistorySyncJobDetail() {
		return JobBuilder.newJob(ViewHistorySyncJob.class)
				.withIdentity("viewHistorySyncJob")
				.storeDurably()
				.requestRecovery(true)
				.build();
	}

}
