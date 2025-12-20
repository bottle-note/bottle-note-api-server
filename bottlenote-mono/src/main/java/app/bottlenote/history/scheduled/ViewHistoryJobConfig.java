package app.bottlenote.history.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "schedules.history.view.sync",
    name = "enable",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class ViewHistoryJobConfig {

  @Value("${schedules.history.view.sync.enable}")
  private boolean enable;

  @Bean
  public Trigger viewHistorySyncJobTrigger() {
    log.info("viewHistorySyncJobTrigger() called {}", enable ? "enable" : "disable");
    return TriggerBuilder.newTrigger()
        .forJob(viewHistorySyncJobDetail())
        .withIdentity("viewHistorySyncTrigger")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 */1 * * * ?"))
        .build();
  }

  @Bean
  public JobDetail viewHistorySyncJobDetail() {
    log.info("viewHistorySyncJobDetail() called {}", enable ? "enable" : "disable");
    return JobBuilder.newJob(ViewHistorySyncJob.class)
        .withIdentity("viewHistorySyncJob")
        .storeDurably()
        .requestRecovery(true)
        .build();
  }
}
