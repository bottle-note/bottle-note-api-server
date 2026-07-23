package app.batch.bottlenote.curation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class CurationExpirationStatusJob extends QuartzJobBean {

  private static final String DEACTIVATE_EXPIRED_CURATIONS_SQL =
      "UPDATE curation "
          + "SET is_active = false, last_modify_at = NOW(), last_modify_by = 'batch-curation-expiration' "
          + "WHERE is_active = true "
          + "AND exposure_end_date IS NOT NULL "
          + "AND exposure_end_date < CURDATE()";

  private final JdbcTemplate jdbcTemplate;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    int updatedCount = jdbcTemplate.update(DEACTIVATE_EXPIRED_CURATIONS_SQL);
    if (updatedCount > 0) {
      log.info("노출 종료일이 지난 큐레이션 비활성화 완료 count={}", updatedCount);
    }
  }
}
