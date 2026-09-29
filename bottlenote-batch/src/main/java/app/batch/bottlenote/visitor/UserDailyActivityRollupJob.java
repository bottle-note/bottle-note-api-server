package app.batch.bottlenote.visitor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class UserDailyActivityRollupJob extends QuartzJobBean {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private final UserDailyActivityRollup rollup;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    LocalDateTime now = LocalDateTime.now(ZONE);
    UserDailyActivityRollup.RollupRange range = rollup.rollup(LocalDate.now(ZONE), now);
    log.info(
        "회원 일별 활동 롤업 완료 from={} toExclusive={} affectedRows={}",
        range.from(),
        range.toExclusive(),
        range.affectedRows());
  }
}
