package app.batch.bottlenote.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** JDBC JobStore는 설정에서 빠진 잡을 남기므로, 폐기된 일간 인기 주류 잡을 기동 시 제거한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyPopularAlcoholJobRemover implements ApplicationRunner {

  static final JobKey LEGACY_JOB_KEY = JobKey.jobKey("popularReviewSelectedJob");

  private final Scheduler scheduler;

  @Override
  public void run(ApplicationArguments args) throws SchedulerException {
    if (!scheduler.checkExists(LEGACY_JOB_KEY)) {
      return;
    }
    scheduler.deleteJob(LEGACY_JOB_KEY);
    log.info("폐기된 인기 주류 Quartz 잡을 제거했습니다. jobKey={}", LEGACY_JOB_KEY);
  }
}
