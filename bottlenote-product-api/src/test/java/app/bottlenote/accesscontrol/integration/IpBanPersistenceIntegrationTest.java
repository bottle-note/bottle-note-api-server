package app.bottlenote.accesscontrol.integration;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse;
import app.bottlenote.accesscontrol.service.IpBanRetentionService;
import app.bottlenote.accesscontrol.service.IpBanService;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.fixture.AdminUserTestFactory;
import app.bottlenote.user.fixture.UserTestFactory;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("integration")
@DisplayName("[integration] IP 차단 영속화")
class IpBanPersistenceIntegrationTest extends IntegrationTestSupport {

  private static final String IPV4 = "198.51.100.20";
  private static final String IPV6 = "2001:db8::20";

  @Autowired private IpBanService ipBanService;
  @Autowired private IpBanRepository ipBanRepository;
  @Autowired private IpBanEventRepository ipBanEventRepository;
  @Autowired private IpSecuritySignalRepository ipSecuritySignalRepository;
  @Autowired private IpBanRetentionService ipBanRetentionService;
  @Autowired private AgentRepository agentRepository;
  @Autowired private AdminUserTestFactory adminUserTestFactory;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("밴 요청은 현재 상태와 BAN 이벤트를 한 트랜잭션으로 저장한다")
  void ban_whenNewIp_persistsCurrentStateAndBanEvent() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();

    IpBanDetailResponse detail =
        ipBanService.ban(IPV4, Duration.ofMinutes(10), "db-abuse", admin.getId());

    assertThat(ipBanRepository.findByNormalizedIp(IPV4)).isPresent();
    List<IpBanAuditRecord> events = ipBanEventRepository.findByIpBanIdOrderByIdAsc(detail.id());
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getEventType()).isEqualTo(IpBanEventType.BAN);
    assertThat(events.getFirst().getActorType()).isEqualTo(IpBanActorType.ADMIN);
    assertThat(events.getFirst().getActorAdminUserId()).isEqualTo(admin.getId());
  }

  @Test
  @DisplayName("활성 재밴은 EXTEND 이벤트를 추가하고 만료 시각을 갱신한다")
  void ban_whenActive_extendsAndAppendsEvent() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    IpBanDetailResponse first =
        ipBanService.ban(IPV4, Duration.ofMinutes(5), "first", admin.getId());

    IpBanDetailResponse second =
        ipBanService.ban(IPV4, Duration.ofMinutes(30), "extend", admin.getId());

    assertThat(second.id()).isEqualTo(first.id());
    assertThat(second.expiresAt()).isAfter(first.expiresAt());
    assertThat(ipBanEventRepository.findByIpBanIdOrderByIdAsc(second.id()))
        .extracting(IpBanAuditRecord::getEventType)
        .containsExactly(IpBanEventType.BAN, IpBanEventType.EXTEND);
  }

  @Test
  @DisplayName("해제와 만료는 상태를 바꾸고 감사 이력을 남긴다")
  void unbanAndExpire_updateStatusAndAppendEvents() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    ipBanService.ban("198.51.100.21", Duration.ofMinutes(10), "u", admin.getId());
    ipBanService.ban("198.51.100.22", Duration.ofMinutes(10), "e", admin.getId());

    IpBanDetailResponse unbanned = ipBanService.unban("198.51.100.21", "manual", admin.getId());
    IpBanDetailResponse expired = ipBanService.expire("198.51.100.22", "ttl");

    assertThat(unbanned.status()).isEqualTo(IpBanStatus.UNBANNED);
    assertThat(expired.status()).isEqualTo(IpBanStatus.EXPIRED);
    assertThat(
            ipBanEventRepository.findByIpBanIdOrderByIdAsc(unbanned.id()).getLast().getEventType())
        .isEqualTo(IpBanEventType.UNBAN);
    assertThat(
            ipBanEventRepository.findByIpBanIdOrderByIdAsc(expired.id()).getLast().getEventType())
        .isEqualTo(IpBanEventType.EXPIRE);
    assertThat(
            ipBanEventRepository.findByIpBanIdOrderByIdAsc(expired.id()).getLast().getActorType())
        .isEqualTo(IpBanActorType.SYSTEM);
  }

  @Test
  @DisplayName("Agent 매핑 관리자의 밴은 AGENT 주체와 agent UUID를 기록한다")
  void ban_whenAdminMappedToAgent_recordsAgentActor() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    var productUser = userTestFactory.persistUser();
    String agentId = UUID.randomUUID().toString();
    agentRepository.save(
        Agent.builder()
            .id(agentId)
            .profileCode("0002")
            .name("Security Agent")
            .status(ACTIVE)
            .productUserId(productUser.getId())
            .adminUserId(admin.getId())
            .apiKeyHash("b".repeat(64))
            .build());

    IpBanDetailResponse detail =
        ipBanService.ban("2001:DB8::20", Duration.ofMinutes(3), "agent", admin.getId());

    assertThat(detail.normalizedIp()).isEqualTo(IPV6);
    IpBanAuditRecord event = ipBanEventRepository.findByIpBanIdOrderByIdAsc(detail.id()).getFirst();
    assertThat(event.getActorType()).isEqualTo(IpBanActorType.AGENT);
    assertThat(event.getActorAdminUserId()).isEqualTo(admin.getId());
    assertThat(event.getActorAgentId()).isEqualTo(agentId);
  }

  @Test
  @DisplayName("목록과 상세 조회는 DB 현재 상태를 원본으로 사용한다")
  void listAndDetail_useDatabaseAsSourceOfTruth() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    ipBanService.ban("198.51.100.30", Duration.ofMinutes(10), "a", admin.getId());
    ipBanService.ban("198.51.100.31", Duration.ofMinutes(10), "b", admin.getId());
    ipBanService.unban("198.51.100.31", "done", admin.getId());

    List<IpBanSummaryResponse> active = ipBanService.list(IpBanStatus.ACTIVE, 100);
    IpBanDetailResponse detail = ipBanService.getDetail("198.51.100.30").orElseThrow();

    assertThat(active)
        .extracting(IpBanSummaryResponse::normalizedIp)
        .containsExactly("198.51.100.30");
    assertThat(detail.events()).isNotEmpty();
    assertThat(detail.status()).isEqualTo(IpBanStatus.ACTIVE);
  }

  @Test
  @DisplayName("180일 이내 signal이 연결된 종료 밴은 보존하고 모두 만료되면 FK 순서로 삭제한다")
  void retention_whenRecentSignalExists_preservesParentUntilSignalExpires() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    IpBanDetailResponse ban =
        ipBanService.ban("198.51.100.40", Duration.ofMinutes(10), "retention", admin.getId());
    ipBanService.expire("198.51.100.40", "expired");
    IpSecuritySignal signal =
        ipSecuritySignalRepository.save(
            IpSecuritySignal.report(
                ban.id(),
                "198.51.100.40",
                "/api/v2/auth",
                "POST",
                "rate-limit",
                ban.effectiveFrom(),
                ban.expiresAt(),
                1,
                null,
                null,
                null));
    jdbcTemplate.update(
        "update ip_bans set state_changed_at = date_sub(now(6), interval 181 day) where id = ?",
        ban.id());
    jdbcTemplate.update(
        "update ip_security_signals set create_at = date_sub(now(6), interval 179 day) where id = ?",
        signal.getId());

    ipBanRetentionService.purgeExpiredData();

    assertThat(ipBanRepository.findById(ban.id())).isPresent();
    assertThat(ipSecuritySignalRepository.findById(signal.getId())).isPresent();
    jdbcTemplate.update(
        "update ip_security_signals set create_at = date_sub(now(6), interval 181 day) where id = ?",
        signal.getId());

    ipBanRetentionService.purgeExpiredData();

    assertThat(ipSecuritySignalRepository.findById(signal.getId())).isEmpty();
    assertThat(ipBanEventRepository.findByIpBanIdOrderByIdAsc(ban.id())).isEmpty();
    assertThat(ipBanRepository.findById(ban.id())).isEmpty();
  }
}
