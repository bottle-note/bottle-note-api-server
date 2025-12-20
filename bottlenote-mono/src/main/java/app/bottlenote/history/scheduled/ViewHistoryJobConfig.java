package app.bottlenote.history.scheduled;

import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = "schedules.history.view.sync.enable",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class ViewHistoryJobConfig {

  @Bean
  public Trigger viewHistorySyncJobTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(viewHistorySyncJobDetail())
        .withIdentity("viewHistorySyncTrigger")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 */1 * * * ?"))
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
