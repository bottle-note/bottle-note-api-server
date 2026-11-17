package app.bottlenote.accesscontrol.scheduled;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanEventRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanRepository;
import app.bottlenote.accesscontrol.service.DefaultIpBanFacade;
import app.bottlenote.accesscontrol.service.IpBanReconciliationService;
import app.bottlenote.accesscontrol.service.IpBanService;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

@Tag("unit")
@DisplayName("IP ban 재조정 Quartz 작업 단위 테스트")
class IpBanReconciliationJobTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Test
  @DisplayName("JobDataMap cursor는 실행마다 다음 200건 위치로 갱신되고 종료 시 제거된다")
  void reconcile_updatesAndResetsInactiveCursorInJobDataMap() {
    InMemoryIpBanRepository banRepository = new InMemoryIpBanRepository();
    InMemoryIpBanEventRepository eventRepository = new InMemoryIpBanEventRepository();
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    for (int host = 1; host <= 401; host++) {
      IpBan ban =
          banRepository.save(
              IpBan.createActive(
                  inactiveTestIp(host),
                  "abuse",
                  NOW.minusMinutes(10),
                  NOW.plusMinutes(10),
                  NOW.minusMinutes(10)));
      ban.unban("reviewed", NOW.minusMinutes(1));
      eventRepository.save(
          IpBanAuditRecord.create(
              ban.getId(),
              IpBanEventType.UNBAN,
              "reviewed",
              NOW.plusMinutes(10),
              NOW.plusMinutes(10),
              IpBanActorType.ADMIN,
              1L,
              null,
              NOW.minusMinutes(1)));
      store.ban(ban.getNormalizedIp(), Duration.ofMinutes(10), "stale");
    }
    IpBanService banService =
        new IpBanService(
            banRepository,
            eventRepository,
            new DefaultAgentFacade(new InMemoryAgentRepository()),
            CLOCK);
    IpBanReconciliationService reconciliationService =
        new IpBanReconciliationService(
            banRepository,
            eventRepository,
            new DefaultIpBanFacade(banService, store),
            store,
            CLOCK);
    IpBanReconciliationJob job = new IpBanReconciliationJob(reconciliationService);
    JobDataMap jobDataMap = new JobDataMap();

    job.reconcile(jobDataMap);
    assertThat(jobDataMap.getString(IpBanReconciliationJob.INACTIVE_CURSOR_ID)).isEqualTo("200");

    job.reconcile(jobDataMap);
    assertThat(jobDataMap.getString(IpBanReconciliationJob.INACTIVE_CURSOR_ID)).isEqualTo("400");

    job.reconcile(jobDataMap);
    assertThat(jobDataMap)
        .doesNotContainKeys(
            IpBanReconciliationJob.INACTIVE_CURSOR_STATE_CHANGED_AT,
            IpBanReconciliationJob.INACTIVE_CURSOR_ID);
    assertThat(store.isBanned(inactiveTestIp(401))).isFalse();
  }

  private static String inactiveTestIp(int index) {
    return "198.20." + (index / 250) + "." + (index % 250 + 1);
  }
}
