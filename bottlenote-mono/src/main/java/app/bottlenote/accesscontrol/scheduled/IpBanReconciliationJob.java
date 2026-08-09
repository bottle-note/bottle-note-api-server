package app.bottlenote.accesscontrol.scheduled;

import app.bottlenote.accesscontrol.service.IpBanReconciliationService;
import app.bottlenote.accesscontrol.service.IpBanReconciliationService.InactiveCursor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class IpBanReconciliationJob implements Job {

  static final String INACTIVE_CURSOR_STATE_CHANGED_AT = "inactiveCursorStateChangedAt";
  static final String INACTIVE_CURSOR_ID = "inactiveCursorId";

  private final IpBanReconciliationService reconciliationService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      reconcile(context.getJobDetail().getJobDataMap());
    } catch (RuntimeException exception) {
      log.error("IP ban reconciliation failed", exception);
      throw new JobExecutionException(exception);
    }
  }

  void reconcile(JobDataMap jobDataMap) {
    InactiveCursor nextCursor = reconciliationService.reconcile(readCursor(jobDataMap));
    writeCursor(jobDataMap, nextCursor);
  }

  private static InactiveCursor readCursor(JobDataMap jobDataMap) {
    String stateChangedAt = jobDataMap.getString(INACTIVE_CURSOR_STATE_CHANGED_AT);
    String id = jobDataMap.getString(INACTIVE_CURSOR_ID);
    if (stateChangedAt == null || id == null) {
      return null;
    }
    return new InactiveCursor(LocalDateTime.parse(stateChangedAt), Long.parseLong(id));
  }

  private static void writeCursor(JobDataMap jobDataMap, InactiveCursor cursor) {
    if (cursor == null) {
      jobDataMap.remove(INACTIVE_CURSOR_STATE_CHANGED_AT);
      jobDataMap.remove(INACTIVE_CURSOR_ID);
      return;
    }
    jobDataMap.put(INACTIVE_CURSOR_STATE_CHANGED_AT, cursor.stateChangedAt().toString());
    jobDataMap.put(INACTIVE_CURSOR_ID, Long.toString(cursor.id()));
  }
}
