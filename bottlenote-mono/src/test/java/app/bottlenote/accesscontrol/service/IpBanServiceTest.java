package app.bottlenote.accesscontrol.service;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse;
import app.bottlenote.accesscontrol.exception.IpBanException;
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanEventRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanRepository;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("IpBanService 단위 테스트")
class IpBanServiceTest {

  private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final Instant NOW = Instant.parse("2026-08-09T03:00:00Z");
  private static final String IPV4 = "203.0.113.10";
  private static final String IPV6 = "2001:db8::1";

  private InMemoryIpBanRepository banRepository;
  private InMemoryIpBanEventRepository eventRepository;
  private InMemoryAgentRepository agentRepository;
  private IpBanService service;

  @BeforeEach
  void setUp() {
    banRepository = new InMemoryIpBanRepository();
    eventRepository = new InMemoryIpBanEventRepository();
    agentRepository = new InMemoryAgentRepository();
    service =
        new IpBanService(
            banRepository,
            eventRepository,
            new DefaultAgentFacade(agentRepository),
            Clock.fixed(NOW, ZONE_ID));
  }

  @Nested
  @DisplayName("차단할 때")
  class Ban {

    @Test
    @DisplayName("신규 IPv4 차단과 BAN 이벤트를 같은 상태로 저장한다")
    void ban_whenNewIpv4_persistsActiveBanAndBanEvent() {
      IpBanDetailResponse detail = service.ban(IPV4, Duration.ofMinutes(10), "abuse", 11L);

      assertThat(detail.normalizedIp()).isEqualTo(IPV4);
      assertThat(detail.status()).isEqualTo(IpBanStatus.ACTIVE);
      assertThat(detail.reason()).isEqualTo("abuse");
      assertThat(detail.events()).hasSize(1);
      assertThat(detail.events().getFirst().eventType()).isEqualTo(IpBanEventType.BAN);
      assertThat(detail.events().getFirst().actorType()).isEqualTo(IpBanActorType.ADMIN);
      assertThat(detail.events().getFirst().actorAdminUserId()).isEqualTo(11L);
      assertThat(detail.events().getFirst().actorAgentId()).isNull();
      assertThat(eventRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("IPv6를 정규화해 저장한다")
    void ban_whenIpv6_normalizesAddress() {
      IpBanDetailResponse detail = service.ban("2001:DB8::1", Duration.ofMinutes(5), "scan", 1L);

      assertThat(detail.normalizedIp()).isEqualTo(IPV6);
      assertThat(banRepository.findByNormalizedIp(IPV6)).isPresent();
    }

    @Test
    @DisplayName("활성 차단에 재요청하면 EXTEND 이벤트로 갱신한다")
    void ban_whenAlreadyActive_extendsAndAppendsExtendEvent() {
      service.ban(IPV4, Duration.ofMinutes(5), "first", 1L);

      IpBanDetailResponse detail = service.ban(IPV4, Duration.ofMinutes(30), "extend-reason", 1L);

      assertThat(detail.status()).isEqualTo(IpBanStatus.ACTIVE);
      assertThat(detail.reason()).isEqualTo("extend-reason");
      assertThat(detail.events())
          .extracting(event -> event.eventType())
          .containsExactly(IpBanEventType.BAN, IpBanEventType.EXTEND);
      assertThat(detail.events().get(1).previousExpiresAt()).isNotNull();
      assertThat(detail.events().get(1).nextExpiresAt())
          .isAfter(detail.events().get(1).previousExpiresAt());
    }

    @Test
    @DisplayName("해제 후 재차단하면 BAN 이벤트를 추가한다")
    void ban_whenUnbanned_reBansWithBanEvent() {
      service.ban(IPV4, Duration.ofMinutes(5), "first", 1L);
      service.unban(IPV4, "done", 1L);

      IpBanDetailResponse detail = service.ban(IPV4, Duration.ofMinutes(15), "again", 1L);

      assertThat(detail.status()).isEqualTo(IpBanStatus.ACTIVE);
      assertThat(detail.events())
          .extracting(event -> event.eventType())
          .containsExactly(IpBanEventType.BAN, IpBanEventType.UNBAN, IpBanEventType.BAN);
      assertThat(banRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Admin에 매핑된 활성 Agent가 있으면 AGENT 주체로 기록한다")
    void ban_whenAdminMappedToAgent_recordsAgentActor() {
      String agentId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
      agentRepository.save(
          Agent.builder()
              .id(agentId)
              .profileCode("0001")
              .name("Agent")
              .status(ACTIVE)
              .productUserId(100L)
              .adminUserId(42L)
              .apiKeyHash("a".repeat(64))
              .build());

      IpBanDetailResponse detail = service.ban(IPV4, Duration.ofMinutes(5), "agent-ban", 42L);

      assertThat(detail.events().getFirst().actorType()).isEqualTo(IpBanActorType.AGENT);
      assertThat(detail.events().getFirst().actorAdminUserId()).isEqualTo(42L);
      assertThat(detail.events().getFirst().actorAgentId()).isEqualTo(agentId);
    }

    @Test
    @DisplayName("adminUserId가 없으면 SYSTEM 주체로 기록한다")
    void ban_whenAdminUserIdNull_recordsSystemActor() {
      IpBanDetailResponse detail = service.ban(IPV4, Duration.ofMinutes(5), "auto", null);

      assertThat(detail.events().getFirst().actorType()).isEqualTo(IpBanActorType.SYSTEM);
      assertThat(detail.events().getFirst().actorAdminUserId()).isNull();
      assertThat(detail.events().getFirst().actorAgentId()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 IP면 예외를 던진다")
    void ban_whenInvalidIp_throws() {
      assertThatThrownBy(() -> service.ban("not-an-ip", Duration.ofMinutes(1), "x", 1L))
          .isInstanceOf(IpBanException.class)
          .extracting("exceptionCode")
          .isEqualTo(IpBanExceptionCode.INVALID_IP);
    }

    @Test
    @DisplayName("TTL이 범위를 벗어나면 예외를 던진다")
    void ban_whenTtlOutOfRange_throws() {
      assertThatThrownBy(() -> service.ban(IPV4, Duration.ZERO, "x", 1L))
          .isInstanceOf(IpBanException.class)
          .extracting("exceptionCode")
          .isEqualTo(IpBanExceptionCode.INVALID_TTL);
      assertThatThrownBy(() -> service.ban(IPV4, Duration.ofDays(31), "x", 1L))
          .isInstanceOf(IpBanException.class)
          .extracting("exceptionCode")
          .isEqualTo(IpBanExceptionCode.INVALID_TTL);
    }
  }

  @Nested
  @DisplayName("해제·만료할 때")
  class UnbanAndExpire {

    @Test
    @DisplayName("활성 차단을 해제하고 UNBAN 이벤트를 남긴다")
    void unban_whenActive_marksUnbanned() {
      service.ban(IPV4, Duration.ofMinutes(10), "abuse", 1L);

      IpBanDetailResponse detail = service.unban(IPV4, "manual-unban", 1L);

      assertThat(detail.status()).isEqualTo(IpBanStatus.UNBANNED);
      assertThat(detail.events().getLast().eventType()).isEqualTo(IpBanEventType.UNBAN);
      assertThat(detail.events().getLast().actorType()).isEqualTo(IpBanActorType.ADMIN);
    }

    @Test
    @DisplayName("SYSTEM이 활성 차단을 만료 처리한다")
    void expire_whenActive_marksExpiredAsSystem() {
      service.ban(IPV4, Duration.ofMinutes(10), "abuse", 1L);

      IpBanDetailResponse detail = service.expire(IPV4, "ttl-elapsed");

      assertThat(detail.status()).isEqualTo(IpBanStatus.EXPIRED);
      assertThat(detail.events().getLast().eventType()).isEqualTo(IpBanEventType.EXPIRE);
      assertThat(detail.events().getLast().actorType()).isEqualTo(IpBanActorType.SYSTEM);
    }

    @Test
    @DisplayName("비활성 차단을 해제하려 하면 예외를 던진다")
    void unban_whenNotActive_throws() {
      service.ban(IPV4, Duration.ofMinutes(10), "abuse", 1L);
      service.unban(IPV4, "done", 1L);

      assertThatThrownBy(() -> service.unban(IPV4, "again", 1L))
          .isInstanceOf(IpBanException.class)
          .extracting("exceptionCode")
          .isEqualTo(IpBanExceptionCode.IP_BAN_NOT_ACTIVE);
    }
  }

  @Nested
  @DisplayName("조회할 때")
  class Query {

    @Test
    @DisplayName("상세 조회에 이벤트 이력을 포함한다")
    void getDetail_returnsEventsInOrder() {
      service.ban(IPV4, Duration.ofMinutes(5), "first", 1L);
      service.ban(IPV4, Duration.ofMinutes(10), "extend", 1L);

      IpBanDetailResponse detail = service.getDetail(IPV4).orElseThrow();

      assertThat(detail.events()).hasSize(2);
      assertThat(detail.events().get(0).eventType()).isEqualTo(IpBanEventType.BAN);
      assertThat(detail.events().get(1).eventType()).isEqualTo(IpBanEventType.EXTEND);
    }

    @Test
    @DisplayName("활성 목록을 상태 변경 시각 역순으로 제한해 반환한다")
    void list_whenActive_returnsLimitedSummaries() {
      service.ban("203.0.113.1", Duration.ofMinutes(5), "a", 1L);
      service.ban("203.0.113.2", Duration.ofMinutes(5), "b", 1L);
      service.ban("203.0.113.3", Duration.ofMinutes(5), "c", 1L);
      service.unban("203.0.113.2", "done", 1L);

      List<IpBanSummaryResponse> active = service.list(IpBanStatus.ACTIVE, 10);

      assertThat(active).hasSize(2);
      assertThat(active)
          .extracting(IpBanSummaryResponse::normalizedIp)
          .containsExactlyInAnyOrder("203.0.113.1", "203.0.113.3");
    }
  }

  @Test
  @DisplayName("기록 시각을 마이크로초로 정규화한다")
  void ban_whenClockHasNanos_truncatesToMicros() {
    Instant withNanos = Instant.parse("2026-08-09T03:00:00.123456789Z");
    service =
        new IpBanService(
            banRepository,
            eventRepository,
            new DefaultAgentFacade(agentRepository),
            Clock.fixed(withNanos, ZONE_ID));

    IpBanDetailResponse detail = service.ban(IPV4, Duration.ofSeconds(30), "nano", 1L);

    LocalDateTime expected =
        LocalDateTime.ofInstant(withNanos, ZONE_ID)
            .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    assertThat(detail.effectiveFrom()).isEqualTo(expected);
    assertThat(detail.stateChangedAt()).isEqualTo(expected);
    assertThat(eventRepository.findAll())
        .extracting(IpBanAuditRecord::getOccurredAt)
        .containsExactly(expected);
  }
}
