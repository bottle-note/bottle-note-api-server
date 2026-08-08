package app.bottlenote.accesscontrol.scheduled;

import app.bottlenote.accesscontrol.service.IpBanReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class IpBanReconciliationJob implements Job {

  private final IpBanReconciliationService reconciliationService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      reconciliationService.reconcile();
    } catch (RuntimeException exception) {
      log.error("IP ban reconciliation failed", exception);
      throw new JobExecutionException(exception);
    }
  }
}
