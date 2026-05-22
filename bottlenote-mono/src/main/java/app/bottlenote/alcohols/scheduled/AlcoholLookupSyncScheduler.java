package app.bottlenote.alcohols.scheduled;

import app.bottlenote.alcohols.service.AlcoholLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "schedules.alcohol.lookup.sync",
    name = "enable",
    havingValue = "true")
public class AlcoholLookupSyncScheduler {
  private final AlcoholLookupService alcoholLookupService;

  @Scheduled(cron = "${schedules.alcohol.lookup.sync.cron:0 */5 * * * *}")
  public void syncLookupSnapshot() {
    int syncedCount = alcoholLookupService.syncSnapshot();
    log.info("Alcohol lookup snapshot 동기화 완료: {}건", syncedCount);
  }
}
