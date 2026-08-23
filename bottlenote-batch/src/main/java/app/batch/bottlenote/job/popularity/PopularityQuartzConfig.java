package app.batch.bottlenote.job.popularity;

import app.batch.bottlenote.job.popularity.PopularityObservationJobConfig.PopularityObservationQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인기도 관측 스케줄.
 *
 * <p>매시 정각에 돈다. Quartz가 JDBC JobStore + 클러스터 모드로 동작하므로 인스턴스가 여럿이어도 한 번만 실행된다.
 *
 * <p>기존 인기 주류 잡과 별개로 등록한다 — 신구가 당분간 공존한다.
 */
@Configuration
public class PopularityQuartzConfig {

  private static final String JOB_KEY = "popularityObservationJob";
  private static final String TRIGGER_KEY = "popularityObservationTrigger";

  /** 매시 정각. 버킷 간격을 바꾸면 이 표현식도 함께 바꿔야 한다. */
  private static final String HOURLY_CRON = "0 0 * * * ?";

  @Bean
  public JobDetail popularityObservationJobDetail() {
    return JobBuilder.newJob(PopularityObservationQuartzJob.class)
        .withIdentity(JOB_KEY)
        .storeDurably()
        .requestRecovery(true)
        .build();
  }

  @Bean
  public Trigger popularityObservationJobTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(popularityObservationJobDetail())
        .withIdentity(TRIGGER_KEY)
        .withSchedule(
            CronScheduleBuilder.cronSchedule(HOURLY_CRON)
                // 인스턴스 재기동 등으로 놓친 실행이 몰려 돌지 않게 한 번만 따라잡는다
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }
}
