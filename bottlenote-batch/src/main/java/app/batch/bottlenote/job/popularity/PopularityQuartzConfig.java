package app.batch.bottlenote.job.popularity;

import app.batch.bottlenote.job.popularity.PopularityMonthlyRollupJobConfig.PopularityMonthlyRollupQuartzJob;
import app.batch.bottlenote.job.popularity.PopularityObservationJobConfig.PopularityObservationQuartzJob;
import app.batch.bottlenote.job.popularity.PopularityWeeklyRollupJobConfig.PopularityWeeklyRollupQuartzJob;
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
 * <p>HOUR·WEEK·MONTH 세 Job을 등록한다. Quartz가 JDBC JobStore + 클러스터 모드로 동작하므로 같은 Job은 인스턴스가 여럿이어도 한
 * 번만 실행된다.
 *
 * <p>기존 인기 주류 잡과 별개로 등록한다 — 신구가 당분간 공존한다.
 */
@Configuration
public class PopularityQuartzConfig {

  private static final String JOB_KEY = "popularityObservationJob";
  private static final String TRIGGER_KEY = "popularityObservationTrigger";
  private static final String WEEKLY_JOB_KEY = "popularityWeeklyRollupJob";
  private static final String WEEKLY_TRIGGER_KEY = "popularityWeeklyRollupTrigger";
  private static final String MONTHLY_JOB_KEY = "popularityMonthlyRollupJob";
  private static final String MONTHLY_TRIGGER_KEY = "popularityMonthlyRollupTrigger";

  /** 닫힌 HOUR 상태의 관측 시점 오차를 최소화하도록 매시 정각에 실행한다. */
  private static final String HOURLY_CRON = "0 0 * * * ?";

  /** 월요일 02:50. 매시 정각 HOUR Job과 자정 일배치의 시작 시점을 피한다. */
  private static final String WEEKLY_CRON = "0 50 2 ? * MON";

  /**
   * 매월 1일 03:50. 직전 달력 월이 닫힌 뒤 처음 도는 시각이다.
   *
   * <p>주간 롤업보다 한 시간 뒤에 둔다. 1일이 월요일이면 두 Job이 같은 HOUR 원본을 읽는데, 정리 Step이 주간 롤업 중에 원본을 지우면 그 주의 값이
   * 어긋난다.
   */
  private static final String MONTHLY_CRON = "0 50 3 1 * ?";

  @Bean
  public JobDetail popularityObservationJobDetail() {
    // 현재 상태로 과거 HOUR를 복원할 수 없으므로 recovery 실행을 등록하지 않는다
    return JobBuilder.newJob(PopularityObservationQuartzJob.class)
        .withIdentity(JOB_KEY)
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger popularityObservationJobTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(popularityObservationJobDetail())
        .withIdentity(TRIGGER_KEY)
        .withSchedule(
            CronScheduleBuilder.cronSchedule(HOURLY_CRON)
                // 지연된 현재 상태를 과거 HOUR에 기록할 수 없으므로 놓친 실행은 건너뛴다
                .withMisfireHandlingInstructionDoNothing())
        .build();
  }

  @Bean
  public JobDetail popularityWeeklyRollupJobDetail() {
    return JobBuilder.newJob(PopularityWeeklyRollupQuartzJob.class)
        .withIdentity(WEEKLY_JOB_KEY)
        .storeDurably()
        .requestRecovery(true)
        .build();
  }

  @Bean
  public Trigger popularityWeeklyRollupJobTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(popularityWeeklyRollupJobDetail())
        .withIdentity(WEEKLY_TRIGGER_KEY)
        .withSchedule(
            CronScheduleBuilder.cronSchedule(WEEKLY_CRON)
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail popularityMonthlyRollupJobDetail() {
    return JobBuilder.newJob(PopularityMonthlyRollupQuartzJob.class)
        .withIdentity(MONTHLY_JOB_KEY)
        .storeDurably()
        .requestRecovery(true)
        .build();
  }

  @Bean
  public Trigger popularityMonthlyRollupJobTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(popularityMonthlyRollupJobDetail())
        .withIdentity(MONTHLY_TRIGGER_KEY)
        .withSchedule(
            CronScheduleBuilder.cronSchedule(MONTHLY_CRON)
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }
}
