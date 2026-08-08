package app.bottlenote.accesscontrol.scheduled;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Product JDBC Quartz cluster에서만 실행되는 IP ban 운영 작업. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app", name = "type", havingValue = "product")
@ConditionalOnExpression("'${bottlenote.access-control.enabled:true}'.equals('true')")
public class IpBanJobConfiguration {

  @Bean
  JobDetail ipBanReconciliationJobDetail() {
    return JobBuilder.newJob(IpBanReconciliationJob.class)
        .withIdentity("ipBanReconciliationJob", "accessControl")
        .storeDurably()
        .requestRecovery(true)
        .build();
  }

  @Bean
  Trigger ipBanReconciliationTrigger(
      @org.springframework.beans.factory.annotation.Qualifier("ipBanReconciliationJobDetail")
          JobDetail ipBanReconciliationJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(ipBanReconciliationJobDetail)
        .withIdentity("ipBanReconciliationTrigger", "accessControl")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 */1 * * * ?"))
        .build();
  }

  @Bean
  JobDetail ipBanRetentionJobDetail() {
    return JobBuilder.newJob(IpBanRetentionJob.class)
        .withIdentity("ipBanRetentionJob", "accessControl")
        .storeDurably()
        .requestRecovery(true)
        .build();
  }

  @Bean
  Trigger ipBanRetentionTrigger(
      @org.springframework.beans.factory.annotation.Qualifier("ipBanRetentionJobDetail")
          JobDetail ipBanRetentionJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(ipBanRetentionJobDetail)
        .withIdentity("ipBanRetentionTrigger", "accessControl")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 15 3 * * ?"))
        .build();
  }
}
