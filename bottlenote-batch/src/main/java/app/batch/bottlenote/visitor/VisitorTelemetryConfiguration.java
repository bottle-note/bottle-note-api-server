package app.batch.bottlenote.visitor;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "batch.visitor-telemetry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(VisitorTelemetryProperties.class)
class VisitorTelemetryConfiguration {

  @Bean
  VisitorTelemetryJdbcWriter visitorTelemetryJdbcWriter(
      JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
    return new VisitorTelemetryJdbcWriter(
        jdbcTemplate, new TransactionTemplate(transactionManager));
  }

  @Bean
  VisitorTelemetryStreamConsumer visitorTelemetryStreamConsumer(
      StringRedisTemplate redisTemplate,
      VisitorTelemetryJdbcWriter writer,
      VisitorTelemetryProperties properties) {
    return new VisitorTelemetryStreamConsumer(redisTemplate, writer, properties);
  }

  @Bean
  JobDetail visitorTelemetryRetentionJobDetail() {
    return JobBuilder.newJob(VisitorTelemetryRetentionJob.class)
        .withIdentity("visitorTelemetryRetentionJob")
        .storeDurably()
        .build();
  }

  @Bean
  Trigger visitorTelemetryRetentionTrigger(JobDetail visitorTelemetryRetentionJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(visitorTelemetryRetentionJobDetail)
        .withIdentity("visitorTelemetryRetentionTrigger")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 30 3 * * ?"))
        .build();
  }
}
