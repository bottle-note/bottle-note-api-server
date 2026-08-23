package app.batch.bottlenote.job.popularity;

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
 * <p>매시 20분에 돈다. Quartz가 JDBC JobStore + 클러스터 모드로 동작하므로 인스턴스가 여럿이어도 한 번만 실행된다.
 *
 * <p>기존 인기 주류 잡과 별개로 등록한다 — 신구가 당분간 공존한다.
 */
@Configuration
public class PopularityQuartzConfig {

  private static final String JOB_KEY = "popularityObservationJob";
  private static final String TRIGGER_KEY = "popularityObservationTrigger";
  private static final String WEEKLY_JOB_KEY = "popularityWeeklyRollupJob";
  private static final String WEEKLY_TRIGGER_KEY = "popularityWeeklyRollupTrigger";

  /**
   * 매시 20분. 버킷 간격을 바꾸면 이 표현식도 함께 바꿔야 한다.
   *
   * <p>정각을 피하는 이유가 둘이다. 하나, 조회 이력은 Redis에서 DB로 매분 0초에 동기화되는데 관측도 정각에 돌면 직전 1분치가 아직 안 넘어온 상태로
   * 세어진다. 그 조회는 뒤늦게 이전 구간 시각으로 기록되므로 다음 버킷에도 잡히지 않아 영구히 유실된다.
   *
   * <p>둘, 기존 일배치(베스트 리뷰·인기 주류)가 매일 0시 정각에 돌고 같은 커넥션 풀을 쓴다. 가까이 붙으면 네 축이 커넥션을 얻지 못해 관측이 실패한다.
   */
  private static final String HOURLY_CRON = "0 20 * * * ?";

  /** 월요일 02:50. 매시 20분 HOUR Job과 자정 일배치의 시작 시점을 피한다. */
  private static final String WEEKLY_CRON = "0 50 2 ? * MON";

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
}
