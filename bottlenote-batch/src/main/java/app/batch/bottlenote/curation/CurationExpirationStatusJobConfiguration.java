package app.batch.bottlenote.curation;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CurationExpirationStatusJobConfiguration {

  private static final String JOB_NAME = "curationExpirationStatusJob";
  private static final String TRIGGER_NAME = "curationExpirationStatusTrigger";

  @Bean
  JobDetail curationExpirationStatusJobDetail() {
    return JobBuilder.newJob(CurationExpirationStatusJob.class)
        .withIdentity(JOB_NAME)
        .storeDurably()
        .build();
  }

  @Bean
  Trigger curationExpirationStatusTrigger(JobDetail curationExpirationStatusJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(curationExpirationStatusJobDetail)
        .withIdentity(TRIGGER_NAME)
        .withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
        .build();
  }
}
