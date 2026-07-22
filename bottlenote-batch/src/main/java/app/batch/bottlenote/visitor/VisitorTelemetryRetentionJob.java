package app.batch.bottlenote.visitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class VisitorTelemetryRetentionJob extends QuartzJobBean {

  private static final int DELETE_CHUNK_SIZE = 10_000;
  private static final String DELETE_SQL =
      "DELETE FROM visitor_telemetry_events "
          + "WHERE occurred_at < DATE_SUB(NOW(), INTERVAL 90 DAY) LIMIT "
          + DELETE_CHUNK_SIZE;

  private final JdbcTemplate jdbcTemplate;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    int totalDeleted = 0;
    int deleted;
    do {
      deleted = jdbcTemplate.update(DELETE_SQL);
      totalDeleted += deleted;
    } while (deleted == DELETE_CHUNK_SIZE);

    log.info("VisitorTelemetry 90일 초과 데이터 정리 완료 count={}", totalDeleted);
  }
}
