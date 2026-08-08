package app.bottlenote.accesscontrol.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanEventRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpSecuritySignalRepository;
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

@Tag("unit")
@DisplayName("IpBan 재조정과 보존 정책 단위 테스트")
class IpBanReconciliationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Test
  @DisplayName("Redis가 비어 있으면 활성 DB 차단을 최신 이벤트 버전으로 복원한다")
  void reconcile_whenRedisIsEmpty_projectsActiveBan() {
    Fixture fixture = fixture();
    fixture.ipBanService.ban("203.0.113.120", Duration.ofMinutes(10), "abuse", 1L);

    fixture.reconciliationService.reconcile();

    assertThat(fixture.store.isBanned("203.0.113.120")).isTrue();
  }

  @Test
  @DisplayName("만료된 ACTIVE는 SYSTEM EXPIRE 이벤트로 전이하고 Redis에서 제거한다")
  void reconcile_whenActiveBanExpired_expiresAndUnprojects() {
    Fixture fixture = fixture();
    IpBan ban =
        fixture.banRepository.save(
            IpBan.createActive(
                "203.0.113.121",
                "abuse",
                NOW.minusMinutes(10),
                NOW.minusSeconds(1),
                NOW.minusMinutes(10)));
    fixture.eventRepository.save(
        IpBanAuditRecord.create(
            ban.getId(),
            IpBanEventType.BAN,
            "abuse",
            null,
            ban.getExpiresAt(),
            IpBanActorType.SYSTEM,
            null,
            null,
            NOW.minusMinutes(10)));
    fixture.store.ban("203.0.113.121", Duration.ofMinutes(10), "abuse");

    fixture.reconciliationService.reconcile();

    assertThat(fixture.banRepository.findByNormalizedIp("203.0.113.121").orElseThrow().getStatus())
        .isEqualTo(IpBanStatus.EXPIRED);
    assertThat(fixture.eventRepository.findByIpBanIdOrderByIdAsc(ban.getId()))
        .extracting(IpBanAuditRecord::getEventType)
        .containsExactly(IpBanEventType.BAN, IpBanEventType.EXPIRE);
    assertThat(fixture.store.isBanned("203.0.113.121")).isFalse();
  }

  @Test
  @DisplayName("201번째 비활성 DB 밴도 커서 순회로 Redis에서 제거한다")
  void reconcile_whenInactiveBansExceedBatch_removesBeyondFirstBatch() {
    Fixture fixture = fixture();

    for (int host = 1; host <= 201; host++) {
      String ip = "198.18.1." + host;
      IpBan ban =
          fixture.banRepository.save(
              IpBan.createActive(
                  ip, "abuse", NOW.minusMinutes(10), NOW.plusMinutes(10), NOW.minusMinutes(10)));
      ban.unban("reviewed", NOW.minusMinutes(1));
      fixture.banRepository.save(ban);
      fixture.eventRepository.save(
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
      fixture.store.ban(ip, Duration.ofMinutes(10), "stale");
    }

    fixture.reconciliationService.reconcile();

    assertThat(fixture.store.isBanned("198.18.1.201")).isFalse();
  }

  @Test
  @DisplayName("180일이 지난 종료 밴은 signal과 event를 먼저 지운 뒤 삭제한다")
  void retention_whenTerminatedBanIsOld_deletesChildrenBeforeBan() {
    Fixture fixture = fixture();
    IpBan ban =
        fixture.banRepository.save(
            IpBan.createActive(
                "203.0.113.122",
                "abuse",
                NOW.minusDays(200),
                NOW.minusDays(199),
                NOW.minusDays(200)));
    ban.expire("expired", NOW.minusDays(181));
    fixture.eventRepository.save(
        IpBanAuditRecord.create(
            ban.getId(),
            IpBanEventType.EXPIRE,
            "expired",
            ban.getExpiresAt(),
            ban.getExpiresAt(),
            IpBanActorType.SYSTEM,
            null,
            null,
            NOW.minusDays(181)));
    fixture.signalRepository.save(
        IpSecuritySignal.report(
            ban.getId(),
            ban.getNormalizedIp(),
            "/api/v2/auth",
            "POST",
            "rate-limit",
            NOW.minusDays(181),
            NOW.minusDays(181),
            1,
            null,
            null,
            null));

    fixture.retentionService.purgeExpiredData();

    assertThat(fixture.banRepository.findByNormalizedIp("203.0.113.122")).isEmpty();
    assertThat(fixture.eventRepository.findAll()).isEmpty();
    assertThat(fixture.signalRepository.findAll()).isEmpty();
  }

  private static Fixture fixture() {
    InMemoryIpBanRepository banRepository = new InMemoryIpBanRepository();
    InMemoryIpBanEventRepository eventRepository = new InMemoryIpBanEventRepository();
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    IpBanService ipBanService =
        new IpBanService(
            banRepository,
            eventRepository,
            new DefaultAgentFacade(new InMemoryAgentRepository()),
            CLOCK);
    DefaultIpBanFacade facade = new DefaultIpBanFacade(ipBanService, store);
    InMemoryIpSecuritySignalRepository signalRepository = new InMemoryIpSecuritySignalRepository();
    return new Fixture(
        banRepository,
        eventRepository,
        signalRepository,
        store,
        ipBanService,
        new IpBanReconciliationService(banRepository, eventRepository, facade, store, CLOCK),
        new IpBanRetentionService(banRepository, eventRepository, signalRepository, CLOCK));
  }

  private record Fixture(
      InMemoryIpBanRepository banRepository,
      InMemoryIpBanEventRepository eventRepository,
      InMemoryIpSecuritySignalRepository signalRepository,
      InMemoryAccessControlStore store,
      IpBanService ipBanService,
      IpBanReconciliationService reconciliationService,
      IpBanRetentionService retentionService) {}
}
