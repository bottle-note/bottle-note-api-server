package app.bottlenote.accesscontrol.scheduled;

import app.bottlenote.accesscontrol.service.IpBanRetentionService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
@ConditionalOnBean(AccessControlStore.class)
public class IpBanRetentionJob implements Job {

  private final IpBanRetentionService retentionService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      retentionService.purgeExpiredData();
    } catch (RuntimeException exception) {
      log.error("IP ban retention failed", exception);
      throw new JobExecutionException(exception);
    }
  }
}
